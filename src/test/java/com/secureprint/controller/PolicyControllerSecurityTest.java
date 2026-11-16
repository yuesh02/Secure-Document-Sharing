package com.secureprint.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

class PolicyControllerSecurityTest {
    @Test void policyEndpointsRequireUserRole() {
        PreAuthorize annotation = PolicyController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('USER')", annotation.value());
    }
}
