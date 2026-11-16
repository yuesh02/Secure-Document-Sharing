package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.exception.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PolicyGuardTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final PolicyGuard guard = new PolicyGuard(clock);

    @Test void checksActionBeforeExpiryAndQuota() {
        AccessPolicy policy = policy(Set.of(PolicyAction.VIEW), Instant.parse("2025-12-31T23:59:00Z"), 1, 1);
        PolicyViolationException ex = assertThrows(PolicyViolationException.class, () -> guard.check(policy, PolicyAction.PRINT));
        assertEquals(ErrorCode.ACTION_NOT_ALLOWED, ex.getCode());
    }

    @Test void checksExpiryBeforePrintQuota() {
        AccessPolicy policy = policy(Set.of(PolicyAction.PRINT), Instant.parse("2025-12-31T23:59:00Z"), 1, 1);
        PolicyViolationException ex = assertThrows(PolicyViolationException.class, () -> guard.check(policy, PolicyAction.PRINT));
        assertEquals(ErrorCode.ACCESS_EXPIRED, ex.getCode());
    }

    @Test void checksPrintQuotaWithControlledClock() {
        AccessPolicy policy = policy(Set.of(PolicyAction.PRINT), Instant.parse("2026-01-01T01:00:00Z"), 1, 1);
        PolicyViolationException ex = assertThrows(PolicyViolationException.class, () -> guard.check(policy, PolicyAction.PRINT));
        assertEquals(ErrorCode.PRINT_LIMIT_REACHED, ex.getCode());
    }

    @Test void allowsActionBeforeExpiryAndQuota() {
        AccessPolicy policy = policy(Set.of(PolicyAction.PRINT), Instant.parse("2026-01-01T01:00:00Z"), 1, 0);
        assertDoesNotThrow(() -> guard.check(policy, PolicyAction.PRINT));
    }

    @Test void editQuotaAndExpiryUseDedicatedCodes() {
        AccessPolicy exhausted = policy(Set.of(PolicyAction.EDIT), Instant.parse("2026-01-01T01:00:00Z"), 1, 0);
        try {
            var field = AccessPolicy.class.getDeclaredField("maxEditCount");
            field.setAccessible(true);
            field.set(exhausted, 0);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
        PolicyViolationException limit = assertThrows(PolicyViolationException.class,
                () -> guard.check(exhausted, PolicyAction.EDIT));
        assertEquals(ErrorCode.EDIT_LIMIT_REACHED, limit.getCode());

        AccessPolicy expired = policy(Set.of(PolicyAction.EDIT), Instant.parse("2025-12-31T23:59:00Z"), 1, 0);
        PolicyViolationException expiry = assertThrows(PolicyViolationException.class,
                () -> guard.check(expired, PolicyAction.EDIT));
        assertEquals(ErrorCode.ACCESS_EXPIRED, expiry.getCode());
    }

    private AccessPolicy policy(Set<PolicyAction> actions, Instant expiry, int maxPrints, int used) {
        User user = new User("Owner", "owner@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());
        Document document = new Document(UUID.randomUUID(), user, "a.pdf", "application/pdf", 1, Instant.now());
        AccessPolicy policy = new AccessPolicy(UUID.randomUUID(), document, "token", maxPrints, null, expiry, user, Instant.now(), actions);
        try {
            Field field = AccessPolicy.class.getDeclaredField("currentPrintCount");
            field.setAccessible(true);
            field.set(policy, used);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
        return policy;
    }
}
