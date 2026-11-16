package com.secureprint.service;

import com.secureprint.dto.ReleasedDocument;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessService {
    private final ActionRequestRepository requests;
    private final AccessPolicyRepository policies;
    private final DocumentKeyRepository keys;
    private final DocumentVersionRepository versions;
    private final CryptoService crypto;
    private final FileStorageService storage;
    private final WatermarkService watermark;
    private final PolicyService policyService;
    private final PolicyGuard guard;
    private final AuditService audit;
    private final UserRepository users;
    private final Clock clock;

    public AccessService(ActionRequestRepository requests, AccessPolicyRepository policies, DocumentKeyRepository keys,
                         DocumentVersionRepository versions, CryptoService crypto, FileStorageService storage,
                         WatermarkService watermark, PolicyService policyService, PolicyGuard guard,
                         AuditService audit, UserRepository users, Clock clock) {
        this.requests=requests; this.policies=policies; this.keys=keys; this.versions=versions; this.crypto=crypto;
        this.storage=storage; this.watermark=watermark; this.policyService=policyService; this.guard=guard;
        this.audit=audit; this.users=users; this.clock=clock;
    }

    @Transactional
    public ReleasedDocument release(Long shopId, UUID requestId) {
        User shop = users.findById(shopId).orElseThrow(this::notFoundUser);
        ActionRequest request = requests.findById(requestId).orElseThrow(() -> {
            audit.recordRejected(shop, null, AuditAction.PRINT_RELEASED, "Request not found");
            return notFound();
        });
        if (!request.getRequestedBy().getId().equals(shopId)) {
            audit.recordRejected(shop, request.getDocument(), AuditAction.PRINT_RELEASED, "Request does not belong to shop");
            throw notFound();
        }
        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        if (requests.claimApproved(requestId, transactionId, now) != 1) {
            audit.recordRejected(request.getRequestedBy(), request.getDocument(), AuditAction.PRINT_RELEASED, "Request is not approved or was already released");
            throw violation(ErrorCode.INVALID_STATE, "Request is not approved or was already released");
        }
        int consumed = policies.consumePrint(request.getPolicy().getId(), now);
        if (consumed != 1) {
            AccessPolicy policy = policies.findById(request.getPolicy().getId()).orElseThrow(this::notFoundPolicy);
            ErrorCode code = !now.isBefore(policy.getExpiryTimestamp()) ? ErrorCode.ACCESS_EXPIRED : ErrorCode.PRINT_LIMIT_REACHED;
            audit.recordRejected(request.getRequestedBy(), request.getDocument(), AuditAction.PRINT_RELEASED, code.name());
            throw violation(code, code == ErrorCode.ACCESS_EXPIRED ? "Access policy has expired" : "Print limit has been reached");
        }
        byte[] plaintext = null;
        byte[] encrypted = null;
        try {
            DocumentVersion version = versions.findByDocumentAndStatus(request.getDocument(), VersionStatus.ACTIVE).orElseThrow(this::notFoundVersion);
            encrypted = storage.read(version.getEncryptedPath());
            SecretKey key = crypto.unwrapKey(keys.findById(request.getDocument().getId()).orElseThrow(this::notFoundKey).getWrappedKey());
            plaintext = crypto.decrypt(encrypted, key);
            byte[] released = watermark.apply(plaintext, transactionId, request.getRequestedBy().getName(), now);
            audit.record(request.getRequestedBy(), request.getDocument(), AuditAction.PRINT_RELEASED, "Print released");
            return new ReleasedDocument(transactionId, released);
        } finally {
            if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
            if (encrypted != null) Arrays.fill(encrypted, (byte) 0);
        }
    }

    @Transactional
    public ReleasedDocument viewOrDownload(Long shopId, String token, PolicyAction action) {
        User shop = users.findById(shopId).orElseThrow(this::notFoundUser);
        AccessPolicy policy;
        try {
            policy = policyService.resolveToken(token);
            guard.check(policy, action);
        } catch (PolicyViolationException ex) {
            audit.recordRejected(shop, null, action == PolicyAction.VIEW ? AuditAction.VIEW_RELEASED : AuditAction.DOWNLOAD_RELEASED, ex.getCode().name() + ": " + ex.getMessage());
            throw ex;
        }
        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        byte[] plaintext = null;
        byte[] encrypted = null;
        try {
            DocumentVersion version = versions.findByDocumentAndStatus(policy.getDocument(), VersionStatus.ACTIVE).orElseThrow(this::notFoundVersion);
            encrypted = storage.read(version.getEncryptedPath());
            SecretKey key = crypto.unwrapKey(keys.findById(policy.getDocument().getId()).orElseThrow(this::notFoundKey).getWrappedKey());
            plaintext = crypto.decrypt(encrypted, key);
            byte[] released = watermark.apply(plaintext, transactionId, shop.getName(), now);
            audit.record(shop, policy.getDocument(), action == PolicyAction.VIEW ? AuditAction.VIEW_RELEASED : AuditAction.DOWNLOAD_RELEASED, "Document released");
            return new ReleasedDocument(transactionId, released);
        } finally {
            if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
            if (encrypted != null) Arrays.fill(encrypted, (byte) 0);
        }
    }

    private PolicyViolationException notFoundUser() { return violation(ErrorCode.NOT_FOUND, "Shop not found"); }
    private PolicyViolationException notFound() { return violation(ErrorCode.NOT_FOUND, "Request not found"); }
    private PolicyViolationException notFoundPolicy() { return violation(ErrorCode.NOT_FOUND, "Policy not found"); }
    private PolicyViolationException notFoundVersion() { return violation(ErrorCode.NOT_FOUND, "Active document version not found"); }
    private PolicyViolationException notFoundKey() { return violation(ErrorCode.NOT_FOUND, "Document key not found"); }
    private PolicyViolationException violation(ErrorCode code, String message) { return new PolicyViolationException(code, message); }
}
