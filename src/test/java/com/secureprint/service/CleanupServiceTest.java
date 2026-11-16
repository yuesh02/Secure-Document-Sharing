package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CleanupServiceTest {
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final AccessPolicyRepository policies = mock(AccessPolicyRepository.class);
    private final DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
    private final DocumentKeyRepository keys = mock(DocumentKeyRepository.class);
    private final ActionRequestRepository requests = mock(ActionRequestRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final AuditService audit = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final CleanupService service = new CleanupService(documents, policies, versions, keys, requests, storage, audit, clock);
    private final User owner = new User("Owner", "owner@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());
    private final Document document = new Document(UUID.randomUUID(), owner, "a.pdf", "application/pdf", 10, Instant.now());

    @Test void expiredPolicyRemovesFilesKeyTokenAndRejectsPendingRequests() {
        AccessPolicy policy = policy(Instant.parse("2025-12-31T23:59:00Z"), 3, 0);
        DocumentVersion version = new DocumentVersion(UUID.randomUUID(), document, 1, "one.bin", VersionStatus.ACTIVE, Instant.now());
        when(policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document)).thenReturn(Optional.of(policy));
        when(versions.findAllByDocument(document)).thenReturn(List.of(version));
        service.cleanup(document);
        verify(storage).delete("one.bin");
        verify(requests).rejectPendingForDocument(eq(document.getId()), contains("Automatically"), any());
        verify(keys).deleteById(document.getId());
        verify(documents).save(document);
        verify(audit).recordSystem(eq(document), eq(AuditAction.AUTO_DELETED), anyString());
        assertTrue(document.isDeleted());
        assertTrue(policy.isRevoked());
        assertNull(policy.getToken());
    }

    @Test void exhaustedPolicyIsCleanedAndPrintlessPolicyIsNotExhausted() {
        AccessPolicy exhausted = policy(Instant.parse("2026-01-01T01:00:00Z"), 3, 3);
        assertTrue(service.isExpiredOrExhausted(exhausted, Instant.now(clock)));
        AccessPolicy printless = new AccessPolicy(UUID.randomUUID(), document, "token", null, null,
                Instant.parse("2026-01-01T01:00:00Z"), owner, Instant.now(), Set.of(PolicyAction.VIEW));
        assertFalse(service.isExpiredOrExhausted(printless, Instant.now(clock)));
    }

    @Test void nonExpiredNonExhaustedPolicyIsUntouched() {
        AccessPolicy policy = policy(Instant.parse("2026-01-01T01:00:00Z"), 3, 2);
        when(policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document)).thenReturn(Optional.of(policy));
        service.cleanup(document);
        verifyNoInteractions(storage, keys, requests, audit, documents);
    }

    @Test void fileFailureLeavesDocumentForRetry() {
        AccessPolicy policy = policy(Instant.parse("2025-12-31T23:59:00Z"), 3, 0);
        DocumentVersion version = new DocumentVersion(UUID.randomUUID(), document, 1, "one.bin", VersionStatus.ACTIVE, Instant.now());
        when(policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document)).thenReturn(Optional.of(policy));
        when(versions.findAllByDocument(document)).thenReturn(List.of(version));
        doThrow(new IllegalStateException("disk")).when(storage).delete("one.bin");
        assertThrows(IllegalStateException.class, () -> service.cleanup(document));
        verify(documents, never()).save(document);
        verify(audit, never()).recordSystem(any(), any(), anyString());
    }

    private AccessPolicy policy(Instant expiry, int max, int used) {
        AccessPolicy policy = new AccessPolicy(UUID.randomUUID(), document, "token", max, null,
                expiry, owner, Instant.now(), Set.of(PolicyAction.PRINT));
        try {
            var field = AccessPolicy.class.getDeclaredField("currentPrintCount");
            field.setAccessible(true);
            field.set(policy, used);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
        return policy;
    }
}
