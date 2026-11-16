package com.secureprint.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.*;
import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolicyServiceTest {
    private final AccessPolicyRepository policies = mock(AccessPolicyRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final User owner = new User("Owner", "owner@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());
    private final Document document = new Document(UUID.randomUUID(), owner, "a.pdf", "application/pdf", 10, Instant.now());
    private final PolicyService service;

    PolicyServiceTest() throws Exception {
        setId(owner, 10L);
        service = new PolicyService(policies, documents, users, audit,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), 100, Duration.ofHours(24));
    }

    @Test void tokenHasExactUrlSafeShapeAndIsUnique() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String token = service.generateAccessToken();
            assertEquals(43, token.length());
            assertTrue(token.matches("[A-Za-z0-9_-]+"));
            assertFalse(token.contains(document.getId().toString()));
            assertTrue(tokens.add(token));
        }
    }

    @Test void invalidPolicyValuesAreRejected() {
        when(documents.findByIdAndOwnerIdAndDeletedFalse(document.getId(), 10L)).thenReturn(Optional.of(document));
        when(users.findById(10L)).thenReturn(Optional.of(owner));
        assertValidation(new PolicyRequest(Set.of(), null, null, 1));
        assertValidation(new PolicyRequest(Set.of(PolicyAction.PRINT), 0, null, 1));
        assertValidation(new PolicyRequest(Set.of(PolicyAction.VIEW), null, null, 1441));
        assertValidation(new PolicyRequest(Set.of(PolicyAction.VIEW), null, 1, 1));
    }

    @Test void nonOwnerCannotCreateReadOrRevoke() {
        UUID policyId = UUID.randomUUID();
        when(documents.findByIdAndOwnerIdAndDeletedFalse(document.getId(), 99L)).thenReturn(Optional.empty());
        assertCode(ErrorCode.NOT_FOUND, () -> service.create(99L, document.getId(),
                new PolicyRequest(Set.of(PolicyAction.VIEW), null, null, 1)));

        AccessPolicy policy = new AccessPolicy(policyId, document, "token", null, null, Instant.now().plusSeconds(100),
                owner, Instant.now(), Set.of(PolicyAction.VIEW));
        when(policies.findById(policyId)).thenReturn(Optional.of(policy));
        assertCode(ErrorCode.NOT_FOUND, () -> service.revoke(99L, policyId));
    }

    @Test void newPolicyRevokesOldAndRevokedTokenCannotResolve() {
        when(documents.findByIdAndOwnerIdAndDeletedFalse(document.getId(), 10L)).thenReturn(Optional.of(document));
        when(users.findById(10L)).thenReturn(Optional.of(owner));
        AccessPolicy old = new AccessPolicy(UUID.randomUUID(), document, "old-token", null, null,
                Instant.now().plusSeconds(100), owner, Instant.now(), Set.of(PolicyAction.VIEW));
        when(policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document)).thenReturn(Optional.of(old));
        PolicyResponse created = service.create(10L, document.getId(), new PolicyRequest(Set.of(PolicyAction.VIEW), null, null, 5));
        assertTrue(old.isRevoked());
        assertNull(old.getToken());
        assertEquals(43, created.token().length());
        when(policies.findByToken("old-token")).thenReturn(Optional.of(old));
        assertCode(ErrorCode.NOT_FOUND, () -> service.resolveToken("old-token"));
    }

    @Test void qrDecodesBackToToken() throws Exception {
        UUID policyId = UUID.randomUUID();
        AccessPolicy policy = new AccessPolicy(policyId, document, "qr-token", null, null,
                Instant.now().plusSeconds(100), owner, Instant.now(), Set.of(PolicyAction.VIEW));
        when(policies.findById(policyId)).thenReturn(Optional.of(policy));
        byte[] png = service.qr(10L, policyId);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))));
        assertEquals("qr-token", result.getText());
    }

    private void assertValidation(PolicyRequest request) {
        assertCode(ErrorCode.VALIDATION_ERROR, () -> service.create(10L, document.getId(), request));
    }
    private void assertCode(ErrorCode code, Runnable action) {
        PolicyViolationException ex = assertThrows(PolicyViolationException.class, action::run);
        assertEquals(code, ex.getCode());
    }
    private static void setId(User user, long id) throws Exception {
        setId((Object) user, id);
    }
    private static void setId(Object entity, Object id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
