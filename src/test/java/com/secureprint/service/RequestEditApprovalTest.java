package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.Instant;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

class RequestEditApprovalTest {
    @Test void approvingEditArchivesActiveAndActivatesPending() throws Exception {
        ActionRequestRepository requests = mock(ActionRequestRepository.class);
        PolicyService policies = mock(PolicyService.class);
        UserRepository users = mock(UserRepository.class);
        AuditService audit = mock(AuditService.class);
        PolicyGuard guard = mock(PolicyGuard.class);
        AccessPolicyRepository policyRepository = mock(AccessPolicyRepository.class);
        DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
        FileStorageService storage = mock(FileStorageService.class);
        User owner = new User("Owner", "owner@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());
        Field ownerId = User.class.getDeclaredField("id");
        ownerId.setAccessible(true);
        ownerId.set(owner, 1L);
        Document document = new Document(UUID.randomUUID(), owner, "a.pdf", "application/pdf", 1, Instant.now());
        AccessPolicy policy = new AccessPolicy(UUID.randomUUID(), document, "token", null, 2,
                Instant.now().plusSeconds(1000), owner, Instant.now(), Set.of(PolicyAction.EDIT));
        ActionRequest request = new ActionRequest(UUID.randomUUID(), document, policy, RequestType.EDIT, owner, Instant.now());
        DocumentVersion active = new DocumentVersion(UUID.randomUUID(), document, 1, "one", VersionStatus.ACTIVE, Instant.now());
        DocumentVersion pending = new DocumentVersion(UUID.randomUUID(), document, 2, "two", VersionStatus.PENDING, Instant.now());
        request.setPendingVersion(pending);
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(policyRepository.consumeEdit(eq(policy.getId()), any())).thenReturn(1);
        when(versions.findByDocumentAndStatus(document, VersionStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(requests.decide(eq(request.getId()), eq("APPROVED"), any(), any())).thenReturn(1);
        RequestService service = new RequestService(requests, policies, users, audit, Clock.systemUTC(), guard,
                policyRepository, versions, storage);
        service.approve(1L, request.getId(), "approved");
        assertEquals(VersionStatus.ARCHIVED, active.getStatus());
        assertEquals(VersionStatus.ACTIVE, pending.getStatus());
        verify(audit).record(owner, document, AuditAction.EDIT_APPROVED, "Edit approved");
    }
}
