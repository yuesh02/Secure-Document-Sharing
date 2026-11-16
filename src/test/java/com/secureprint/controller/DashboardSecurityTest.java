package com.secureprint.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

class DashboardSecurityTest {
    @Test void adminEndpointsRequireAdmin() throws Exception {
        assertEquals("hasRole('ADMIN')", AdminController.class.getMethod("stats").getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('ADMIN')", AdminAuditController.class.getAnnotation(PreAuthorize.class).value());
    }
    @Test void ownerDashboardRequiresUser() {
        assertEquals("hasRole('USER')", OwnerDashboardController.class.getAnnotation(PreAuthorize.class).value());
    }
    @Test void shopRequestDashboardRequiresShop() throws Exception {
        assertEquals("hasRole('PRINT_SHOP')", RequestController.class.getMethod("mineFiltered",Long.class,com.secureprint.entity.RequestStatus.class,int.class,int.class)
                .getAnnotation(PreAuthorize.class).value());
    }
    @Test void adminHasNoDecryptedContentRoute() {
        for (var method: AdminController.class.getDeclaredMethods()) {
            assertFalse(method.getName().toLowerCase().contains("content"));
        }
    }
}
