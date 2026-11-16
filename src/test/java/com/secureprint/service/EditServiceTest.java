package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditServiceTest {
    @Test void submittedEditCreatesPendingVersionAndLeavesActiveVersionUntouched() throws Exception {
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
        ActionRequestRepository requests = mock(ActionRequestRepository.class);
        DocumentKeyRepository keys = mock(DocumentKeyRepository.class);
        UserRepository users = mock(UserRepository.class);
        PolicyService policyService = mock(PolicyService.class);
        PolicyGuard guard = mock(PolicyGuard.class);
        CryptoService crypto = new CryptoService(Base64.getEncoder().encodeToString(new byte[32]));
        FileStorageService storage = mock(FileStorageService.class);
        EditStrategy strategy = mock(EditStrategy.class);
        AuditService audit = mock(AuditService.class);
        User shop = new User("Shop", "shop@example.com", "hash", Role.PRINT_SHOP, UserStatus.ACTIVE, Instant.now());
        setId(shop, 2L);
        User owner = new User("Owner", "owner@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());
        setId(owner, 1L);
        Document document = new Document(UUID.randomUUID(), owner, "a.pdf", "application/pdf", 10, Instant.now());
        AccessPolicy policy = new AccessPolicy(UUID.randomUUID(), document, "token", null, 2,
                Instant.now().plusSeconds(1000), owner, Instant.now(), Set.of(PolicyAction.EDIT));
        DocumentVersion active = new DocumentVersion(UUID.randomUUID(), document, 1, "active.bin", VersionStatus.ACTIVE, Instant.now());
        SecretKey key = crypto.generateKey();
        byte[] encrypted = crypto.encrypt("original".getBytes(), key);
        when(users.findById(2L)).thenReturn(Optional.of(shop));
        when(policyService.resolveToken("token")).thenReturn(policy);
        when(versions.findByDocumentAndStatus(document, VersionStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(keys.findById(document.getId())).thenReturn(Optional.of(new DocumentKey(document, crypto.wrapKey(key), Instant.now())));
        when(storage.read("active.bin")).thenReturn(encrypted);
        when(strategy.apply(any(), any())).thenReturn("edited".getBytes());
        when(storage.store(any())).thenReturn("pending.bin");
        when(versions.findTopByDocumentOrderByVersionNumberDesc(document)).thenReturn(Optional.of(active));
        EditService service = new EditService(documents, versions, requests, keys, users, policyService, guard, crypto,
                storage, strategy, audit, Clock.systemUTC(), 50, 500, 6, 72);

        ActionRequestResponse response = service.submit(2L, new EditRequest("token", "note",
                List.of(new OverlayRequest(1, 10f, 10f, "Edited", 12f))));

        assertNotNull(response);
        ArgumentCaptor<DocumentVersion> versionCaptor = ArgumentCaptor.forClass(DocumentVersion.class);
        verify(versions).save(versionCaptor.capture());
        assertEquals(VersionStatus.PENDING, versionCaptor.getValue().getStatus());
        assertEquals(VersionStatus.ACTIVE, active.getStatus());
        ArgumentCaptor<byte[]> storedCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storage).store(storedCaptor.capture());
        assertFalse(new String(storedCaptor.getValue()).contains("edited"));
        verify(audit).record(shop, document, AuditAction.EDIT_SUBMITTED, "Edit submitted");
    }

    private static void setId(User user, long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
