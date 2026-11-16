package com.secureprint.repository;
import com.secureprint.entity.AuditLog;
import com.secureprint.entity.AuditAction;
import com.secureprint.entity.AuditStatus;
import org.springframework.data.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.time.Instant;

public interface AuditLogRepository extends Repository<AuditLog, UUID> {
    AuditLog save(AuditLog auditLog);
    Page<AuditLog> findByDocumentIdOrderByTimestampDesc(UUID documentId, Pageable pageable);
    Page<AuditLog> findByDocumentOwnerIdOrderByTimestampDesc(Long ownerId, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("""
        select a from AuditLog a
        where (:actorId is null or a.actor.id = :actorId)
          and (:documentId is null or a.document.id = :documentId)
          and (:action is null or a.action = :action)
          and (:status is null or a.status = :status)
          and (:fromTime is null or a.timestamp >= :fromTime)
          and (:toTime is null or a.timestamp <= :toTime)
        order by a.timestamp desc
        """)
    Page<AuditLog> search(Long actorId, UUID documentId, AuditAction action, AuditStatus status,
                          Instant fromTime, Instant toTime, Pageable pageable);
    long countByActionAndTimestampAfter(AuditAction action, Instant timestamp);
    long countByActionAndStatusAndTimestampAfter(AuditAction action, AuditStatus status, Instant timestamp);
    long countByStatusAndTimestampAfter(AuditStatus status, Instant timestamp);
}
