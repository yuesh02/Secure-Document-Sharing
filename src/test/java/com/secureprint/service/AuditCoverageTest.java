package com.secureprint.service;

import com.secureprint.entity.*;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuditCoverageTest {
    @Test void allPhaseEventsHaveNamedAuditActions() {
        assertEquals(EnumSet.allOf(AuditAction.class), EnumSet.of(
                AuditAction.SIGNUP, AuditAction.LOGIN_SUCCESS, AuditAction.LOGIN_FAILED,
                AuditAction.ADMIN_STATUS_CHANGE, AuditAction.UPLOAD, AuditAction.DOCUMENT_DELETED,
                AuditAction.POLICY_CREATED, AuditAction.POLICY_REVOKED, AuditAction.REQUEST_CREATED,
                AuditAction.REQUEST_APPROVED, AuditAction.REQUEST_REJECTED, AuditAction.PRINT_RELEASED,
                AuditAction.VIEW_RELEASED, AuditAction.DOWNLOAD_RELEASED, AuditAction.AUTO_DELETED,
                AuditAction.EDIT_SUBMITTED, AuditAction.EDIT_APPROVED, AuditAction.EDIT_REJECTED,
                AuditAction.VERSION_VIEWED));
    }
}
