package com.secureprint.dto;
import com.secureprint.entity.AuditAction;
import com.secureprint.entity.AuditStatus;
import java.time.Instant;
import java.util.UUID;
public record AuditLogResponse(UUID id, Long actorId, UUID documentId, AuditAction action,
                               AuditStatus status, String details, Instant timestamp) {}
