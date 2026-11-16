package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CleanupService {
    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);
    private final DocumentRepository documents;
    private final AccessPolicyRepository policies;
    private final DocumentVersionRepository versions;
    private final DocumentKeyRepository keys;
    private final ActionRequestRepository requests;
    private final FileStorageService storage;
    private final AuditService audit;
    private final Clock clock;

    public CleanupService(DocumentRepository documents, AccessPolicyRepository policies,
                          DocumentVersionRepository versions, DocumentKeyRepository keys,
                          ActionRequestRepository requests, FileStorageService storage,
                          AuditService audit, Clock clock) {
        this.documents=documents; this.policies=policies; this.versions=versions; this.keys=keys;
        this.requests=requests; this.storage=storage; this.audit=audit; this.clock=clock;
    }

    @Transactional
    public void cleanup(Document document) {
        AccessPolicy policy = policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document).orElse(null);
        if (policy == null || document.isDeleted() || !isExpiredOrExhausted(policy, Instant.now(clock))) return;
        for (DocumentVersion version : versions.findAllByDocument(document)) {
            storage.delete(version.getEncryptedPath());
        }
        requests.rejectPendingForDocument(document.getId(), "Automatically rejected because document access expired or quota was exhausted", Instant.now(clock));
        policy.revoke();
        policies.save(policy);
        keys.deleteById(document.getId());
        document.markDeleted(Instant.now(clock));
        documents.save(document);
        audit.recordSystem(document, AuditAction.AUTO_DELETED, "Document automatically deleted after policy expiry or print exhaustion");
    }

    public boolean isExpiredOrExhausted(AccessPolicy policy, Instant now) {
        return !now.isBefore(policy.getExpiryTimestamp())
                || (policy.getMaxPrintCount() != null && policy.getCurrentPrintCount() >= policy.getMaxPrintCount());
    }

    public void logFailure(Document document, RuntimeException failure) {
        log.error("Automatic cleanup failed for document {}; it will be retried", document.getId(), failure);
    }
}
