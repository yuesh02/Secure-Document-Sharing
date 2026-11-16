package com.secureprint.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

class AuditSecurityTest {
    @Test void adminAuditIsAdminOnly() {
        assertEquals("hasRole('ADMIN')", AdminAuditController.class.getAnnotation(PreAuthorize.class).value());
    }
    @Test void documentAuditIsUserOnly() throws Exception {
        assertEquals("hasRole('USER')", AuditController.class.getMethod("documentAudit", Long.class, java.util.UUID.class, int.class, int.class)
                .getAnnotation(PreAuthorize.class).value());
    }
}
