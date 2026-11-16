package com.secureprint.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

class RequestSecurityTest {
    @Test void requestCreationAndReleaseAreShopOnly() throws Exception {
        assertEquals("hasRole('PRINT_SHOP')", RequestController.class.getMethod("create", Long.class, com.secureprint.dto.CreateRequest.class)
                .getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('PRINT_SHOP')", AccessController.class.getMethod("print", Long.class, java.util.UUID.class)
                .getAnnotation(PreAuthorize.class).value());
    }
}
