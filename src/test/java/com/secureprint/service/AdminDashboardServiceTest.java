package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDashboardServiceTest {
    @Test void adminDocumentsExposeMetadataOnly() {
        DocumentRepository documents=mock(DocumentRepository.class);
        UserRepository users=mock(UserRepository.class);
        ActionRequestRepository requests=mock(ActionRequestRepository.class);
        AccessPolicyRepository policies=mock(AccessPolicyRepository.class);
        AuditLogRepository audits=mock(AuditLogRepository.class);
        when(documents.findAllBy(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));
        AdminDashboardService service=new AdminDashboardService(users,documents,requests,policies,audits,Clock.systemUTC());
        var page=service.documents(0,200);
        var pageable=org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(documents).findAllBy(pageable.capture());
        assertEquals(100,pageable.getValue().getPageSize());
    }

    @Test void statsSeparateSuccessfulPrintsFromRejectedAttempts() {
        UserRepository users=mock(UserRepository.class);
        DocumentRepository documents=mock(DocumentRepository.class);
        ActionRequestRepository requests=mock(ActionRequestRepository.class);
        AccessPolicyRepository policies=mock(AccessPolicyRepository.class);
        AuditLogRepository audits=mock(AuditLogRepository.class);
        when(users.count()).thenReturn(4L);
        when(users.countByRole(Role.USER)).thenReturn(2L);
        when(users.countByRole(Role.PRINT_SHOP)).thenReturn(1L);
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);
        when(documents.countByDeletedFalse()).thenReturn(3L);
        when(documents.countByDeletedTrue()).thenReturn(1L);
        when(requests.countByStatus(RequestStatus.PENDING)).thenReturn(2L);
        when(audits.countByActionAndStatusAndTimestampAfter(eq(AuditAction.PRINT_RELEASED),eq(AuditStatus.SUCCESS),any())).thenReturn(5L);
        when(audits.countByStatusAndTimestampAfter(eq(AuditStatus.REJECTED),any())).thenReturn(7L);
        var stats=new AdminDashboardService(users,documents,requests,policies,audits,Clock.systemUTC()).stats();
        assertEquals(5,stats.printsReleased());
        assertEquals(7,stats.rejectedAttemptsLast24Hours());
    }
}
