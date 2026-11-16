package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final UserRepository users;
    private final DocumentRepository documents;
    private final ActionRequestRepository requests;
    private final AccessPolicyRepository policies;
    private final AuditLogRepository audits;
    private final Clock clock;
    public AdminDashboardService(UserRepository users, DocumentRepository documents, ActionRequestRepository requests,
                                 AccessPolicyRepository policies, AuditLogRepository audits, Clock clock) {
        this.users=users; this.documents=documents; this.requests=requests; this.policies=policies; this.audits=audits; this.clock=clock;
    }
    @Transactional(readOnly=true)
    public PageResponse<UserResponse> users(Role role, UserStatus status, int page, int size) {
        Page<User> result;
        Pageable pageable=pageable(page,size);
        if(role!=null && status!=null) result=users.findByRoleAndStatus(role,status,pageable);
        else if(role!=null) result=users.findByRole(role,pageable);
        else if(status!=null) result=users.findByStatus(status,pageable);
        else result=users.findAllBy(pageable);
        return PageResponse.of(result.map(u -> new UserResponse(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getStatus(),u.getCreatedAt())));
    }
    @Transactional(readOnly=true)
    public PageResponse<AdminDocumentResponse> documents(int page,int size) {
        return PageResponse.of(documents.findAllBy(pageable(page,size)).map(d -> new AdminDocumentResponse(
                d.getId(), d.getOwner().getId(), d.getFilename(), d.getContentType(), d.getSizeBytes(),
                d.getUploadedAt(), d.isDeleted(), d.getDeletedAt())));
    }
    @Transactional(readOnly=true)
    public DashboardStatsResponse stats() {
        Instant now=Instant.now(clock);
        return new DashboardStatsResponse(users.count(), users.countByRole(Role.USER), users.countByRole(Role.PRINT_SHOP),
                users.countByRole(Role.ADMIN), documents.countByDeletedFalse(), documents.countByDeletedTrue(),
                requests.countByStatus(RequestStatus.PENDING), audits.countByActionAndStatusAndTimestampAfter(AuditAction.PRINT_RELEASED, AuditStatus.SUCCESS, now.minus(24,java.time.temporal.ChronoUnit.HOURS)),
                audits.countByStatusAndTimestampAfter(AuditStatus.REJECTED, now.minus(24,java.time.temporal.ChronoUnit.HOURS)));
    }
    private Pageable pageable(int page,int size){return PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100),Sort.by("createdAt").descending());}
}
