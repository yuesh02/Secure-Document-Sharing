package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
    private final AuditLogRepository audits;
    private final DocumentRepository documents;
    public AuditQueryService(AuditLogRepository audits, DocumentRepository documents) {
        this.audits=audits; this.documents=documents;
    }
    @Transactional(readOnly=true)
    public AuditPageResponse forOwner(Long ownerId, UUID documentId, int page, int size) {
        if (!documents.findByIdAndOwnerId(documentId, ownerId).isPresent()) {
            throw new PolicyViolationException(ErrorCode.NOT_FOUND, "Document not found");
        }
        return toPage(audits.findByDocumentIdOrderByTimestampDesc(documentId, pageable(page,size)));
    }
    @Transactional(readOnly=true)
    public AuditPageResponse search(Long actorId, UUID documentId, AuditAction action, AuditStatus status,
                                    Instant from, Instant to, int page, int size) {
        return toPage(audits.search(actorId, documentId, action, status, from, to, pageable(page,size)));
    }
    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size),100));
    }
    private AuditPageResponse toPage(Page<AuditLog> page) {
        return new AuditPageResponse(page.map(a -> new AuditLogResponse(a.getId(),
                a.getActor()==null?null:a.getActor().getId(),
                a.getDocument()==null?null:a.getDocument().getId(),
                a.getAction(), a.getStatus(), a.getDetails(), a.getTimestamp())).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
