package com.secureprint.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

class EndpointAuthorizationTest {
    @Test
    void everyNonAuthControllerMappingHasMethodSecurity() throws Exception {
        var classes = new Class<?>[] {
                com.secureprint.controller.AdminAuditController.class,
                com.secureprint.controller.AdminController.class,
                com.secureprint.controller.AccessController.class,
                com.secureprint.controller.AuditController.class,
                com.secureprint.controller.DocumentController.class,
                com.secureprint.controller.EditController.class,
                com.secureprint.controller.OwnerDashboardController.class,
                com.secureprint.controller.OwnerRequestController.class,
                com.secureprint.controller.PolicyController.class,
                com.secureprint.controller.PublicAccessController.class,
                com.secureprint.controller.RequestController.class,
                com.secureprint.controller.VersionController.class
        };
        assertTrue(classes.length > 0);
        for (Class<?> type : classes) {
            String prefix = mapping(type);
            for (Method method : type.getDeclaredMethods()) {
                if (mapping(method) != null && !hasPreAuthorize(type, method)) {
                    throw new AssertionError(type.getName() + "#" + method.getName()
                            + " maps " + prefix + mapping(method) + " without @PreAuthorize");
                }
            }
        }
    }

    private static boolean hasPreAuthorize(Class<?> type, Method method) {
        return method.isAnnotationPresent(PreAuthorize.class)
                || type.isAnnotationPresent(PreAuthorize.class);
    }

    private static String mapping(Class<?> type) {
        RequestMapping mapping = type.getAnnotation(RequestMapping.class);
        return mapping == null || mapping.value().length == 0 ? "" : mapping.value()[0];
    }

    private static String mapping(Method method) {
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping != null && mapping.value().length > 0) return mapping.value()[0];
        for (var annotation : method.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().endsWith("Mapping")) {
                try {
                    String[] value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
                    return value.length == 0 ? "" : value[0];
                } catch (ReflectiveOperationException ignored) {
                    return "";
                }
            }
        }
        return null;
    }
}
