package com.secureprint.service;
import com.secureprint.entity.*;
import com.secureprint.repository.AuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AuditService {
    private final AuditLogRepository repository; private final Clock clock;
    public AuditService(AuditLogRepository repository, Clock clock) { this.repository=repository; this.clock=clock; }
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(User actor, Document document, AuditAction action, String details) {
        repository.save(new AuditLog(actor, document, action, AuditStatus.SUCCESS, details, Instant.now(clock)));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void recordRejected(User actor, Document document, AuditAction action, String reason) {
        repository.save(new AuditLog(actor, document, action, AuditStatus.REJECTED, reason, Instant.now(clock)));
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void recordSystem(Document document, AuditAction action, String details) {
        repository.save(new AuditLog(null, document, action, AuditStatus.SYSTEM, details, Instant.now(clock)));
    }
}
