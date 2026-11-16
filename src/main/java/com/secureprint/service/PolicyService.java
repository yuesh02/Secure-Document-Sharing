package com.secureprint.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {
    private static final Set<PolicyAction> ALL_ACTIONS = Set.of(PolicyAction.values());
    private final AccessPolicyRepository policies;
    private final DocumentRepository documents;
    private final UserRepository users;
    private final AuditService audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final int maxPrintCount;
    private final int maxExpiryMinutes;

    public PolicyService(AccessPolicyRepository policies, DocumentRepository documents, UserRepository users,
                         AuditService audit, Clock clock,
                         @Value("${app.policy-max-print-count}") int maxPrintCount,
                         @Value("${app.policy-max-expiry}") java.time.Duration maxExpiry) {
        this.policies = policies; this.documents = documents; this.users = users; this.audit = audit; this.clock = clock;
        this.maxPrintCount = maxPrintCount;
        this.maxExpiryMinutes = Math.toIntExact(maxExpiry.toMinutes());
    }

    @Transactional
    public PolicyResponse create(Long ownerId, UUID documentId, PolicyRequest request) {
        validate(request);
        Document document = ownedDocument(ownerId, documentId);
        User owner = users.findById(ownerId).orElseThrow(() -> notFound("Owner not found"));
        policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document).ifPresent(old -> {
            old.revoke();
            policies.save(old);
        });
        Instant now = Instant.now(clock);
        AccessPolicy policy = new AccessPolicy(UUID.randomUUID(), document, generateUniqueToken(), request.maxPrintCount(),
                request.maxEditCount(), now.plus(request.expiryMinutes(), ChronoUnit.MINUTES), owner, now, request.allowedActions());
        policies.save(policy);
        audit.record(owner, document, AuditAction.POLICY_CREATED, "Access policy created");
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse get(Long ownerId, UUID documentId) {
        Document document = ownedDocument(ownerId, documentId);
        return policies.findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(document)
                .map(this::toResponse)
                .orElseThrow(() -> notFound("Policy not found"));
    }

    @Transactional
    public void revoke(Long ownerId, UUID policyId) {
        AccessPolicy policy = ownedPolicy(ownerId, policyId);
        policy.revoke();
        policies.save(policy);
        audit.record(policy.getDocument().getOwner(), policy.getDocument(), AuditAction.POLICY_REVOKED, "Access policy revoked");
    }

    @Transactional(readOnly = true)
    public AccessPolicy resolveToken(String token) {
        return policies.findByToken(token)
                .filter(policy -> !policy.isRevoked() && !policy.getDocument().isDeleted())
                .orElseThrow(() -> notFound("Policy not found"));
    }

    @Transactional(readOnly = true)
    public byte[] qr(Long ownerId, UUID policyId) {
        AccessPolicy policy = ownedPolicy(ownerId, policyId);
        if (policy.isRevoked() || policy.getToken() == null) throw notFound("Policy not found");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(policy.getToken(), BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate policy QR code", ex);
        }
    }

    public String generateAccessToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = generateAccessToken();
        } while (policies.findByToken(token).isPresent());
        return token;
    }

    private void validate(PolicyRequest request) {
        if (request.allowedActions() == null || request.allowedActions().isEmpty()
                || !ALL_ACTIONS.containsAll(request.allowedActions())) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "Allowed actions must be a non-empty subset");
        }
        if (request.allowedActions().contains(PolicyAction.PRINT)
                && (request.maxPrintCount() == null || request.maxPrintCount() < 1
                || request.maxPrintCount() > maxPrintCount)) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "A valid maxPrintCount is required for PRINT");
        }
        if (!request.allowedActions().contains(PolicyAction.PRINT) && request.maxPrintCount() != null) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "maxPrintCount requires PRINT");
        }
        if (request.expiryMinutes() == null || request.expiryMinutes() < 1 || request.expiryMinutes() > maxExpiryMinutes) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "Expiry is outside the configured range");
        }
        if (!request.allowedActions().contains(PolicyAction.EDIT) && request.maxEditCount() != null) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "maxEditCount requires EDIT");
        }
        if (request.maxEditCount() != null && request.maxEditCount() < 1) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "maxEditCount must be positive");
        }
    }

    private Document ownedDocument(Long ownerId, UUID documentId) {
        return documents.findByIdAndOwnerIdAndDeletedFalse(documentId, ownerId)
                .orElseThrow(() -> notFound("Document not found"));
    }

    private AccessPolicy ownedPolicy(Long ownerId, UUID policyId) {
        AccessPolicy policy = policies.findById(policyId).orElseThrow(() -> notFound("Policy not found"));
        if (!policy.getDocument().getOwner().getId().equals(ownerId) || policy.getDocument().isDeleted()) {
            throw notFound("Policy not found");
        }
        return policy;
    }

    private PolicyViolationException notFound(String message) {
        return new PolicyViolationException(ErrorCode.NOT_FOUND, message);
    }

    private PolicyResponse toResponse(AccessPolicy policy) {
        return new PolicyResponse(policy.getId(), policy.getActions(), policy.getToken(), policy.getMaxPrintCount(),
                policy.getCurrentPrintCount(), policy.getMaxEditCount(), policy.getCurrentEditCount(),
                policy.getExpiryTimestamp(), policy.isRevoked());
    }
}
