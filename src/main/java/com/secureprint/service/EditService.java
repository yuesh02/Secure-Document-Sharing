package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditService {
    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final ActionRequestRepository requests;
    private final DocumentKeyRepository keys;
    private final UserRepository users;
    private final PolicyService policies;
    private final PolicyGuard guard;
    private final CryptoService crypto;
    private final FileStorageService storage;
    private final EditStrategy strategy;
    private final AuditService audit;
    private final Clock clock;
    private final int maxOverlays;
    private final int maxTextLength;
    private final float minFontSize;
    private final float maxFontSize;

    public EditService(DocumentRepository documents, DocumentVersionRepository versions,
                       ActionRequestRepository requests, DocumentKeyRepository keys, UserRepository users,
                       PolicyService policies, PolicyGuard guard, CryptoService crypto,
                       FileStorageService storage, EditStrategy strategy, AuditService audit, Clock clock,
                       @Value("${app.edit.max-overlays}") int maxOverlays,
                       @Value("${app.edit.max-text-length}") int maxTextLength,
                       @Value("${app.edit.min-font-size}") float minFontSize,
                       @Value("${app.edit.max-font-size}") float maxFontSize) {
        this.documents=documents; this.versions=versions; this.requests=requests; this.keys=keys; this.users=users;
        this.policies=policies; this.guard=guard; this.crypto=crypto; this.storage=storage; this.strategy=strategy;
        this.audit=audit; this.clock=clock; this.maxOverlays=maxOverlays; this.maxTextLength=maxTextLength;
        this.minFontSize=minFontSize; this.maxFontSize=maxFontSize;
    }

    @Transactional
    public ActionRequestResponse submit(Long shopId, EditRequest input) {
        User shop = users.findById(shopId).orElseThrow(() -> notFound("Shop not found"));
        AccessPolicy policy;
        try {
            policy = policies.resolveToken(input.token());
            guard.check(policy, PolicyAction.EDIT);
            validateOverlays(input);
            if (requests.existsByDocumentAndTypeAndStatus(policy.getDocument(), RequestType.EDIT, RequestStatus.PENDING)) {
                throw violation(ErrorCode.INVALID_STATE, "A pending edit already exists");
            }
            DocumentVersion active = versions.findByDocumentAndStatus(policy.getDocument(), VersionStatus.ACTIVE)
                    .orElseThrow(() -> notFound("Active document version not found"));
            byte[] encrypted = null;
            byte[] plaintext = null;
            String path = null;
            try {
                encrypted = storage.read(active.getEncryptedPath());
                SecretKey key = crypto.unwrapKey(keys.findById(policy.getDocument().getId())
                        .orElseThrow(() -> notFound("Document key not found")).getWrappedKey());
                plaintext = crypto.decrypt(encrypted, key);
                byte[] edited = strategy.apply(plaintext, input.overlays());
                try {
                    path = storage.store(crypto.encrypt(edited, key));
                } finally {
                    Arrays.fill(edited, (byte) 0);
                }
            } finally {
                if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
                if (encrypted != null) Arrays.fill(encrypted, (byte) 0);
            }
            Instant now = Instant.now(clock);
            int versionNumber = versions.findTopByDocumentOrderByVersionNumberDesc(policy.getDocument()).map(v -> v.getVersionNumber() + 1).orElse(1);
            DocumentVersion pending = new DocumentVersion(UUID.randomUUID(), policy.getDocument(), versionNumber, path, VersionStatus.PENDING, now);
            pending.setEditedBy(shop);
            pending.setChangeNote(input.changeNote());
            try {
                versions.save(pending);
                ActionRequest request = new ActionRequest(UUID.randomUUID(), policy.getDocument(), policy, RequestType.EDIT, shop, now);
                request.setPendingVersion(pending);
                requests.save(request);
                audit.record(shop, policy.getDocument(), AuditAction.EDIT_SUBMITTED, "Edit submitted");
                return toResponse(request);
            } catch (RuntimeException ex) {
                storage.delete(path);
                throw ex;
            }
        } catch (PolicyViolationException ex) {
            audit.recordRejected(shop, policyOrNull(input.token()), AuditAction.EDIT_SUBMITTED, ex.getCode() + ": " + ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly=true)
    public List<VersionResponse> versions(Long ownerId, UUID documentId) {
        Document document = ownedDocument(ownerId, documentId);
        return versions.findAllByDocument(document).stream()
                .sorted(Comparator.comparing(DocumentVersion::getVersionNumber))
                .map(v -> new VersionResponse(v.getId(), v.getVersionNumber(), v.getStatus(), v.getChangeNote(), v.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public byte[] content(Long ownerId, UUID documentId, UUID versionId) {
        Document document = ownedDocument(ownerId, documentId);
        DocumentVersion version = versions.findById(versionId)
                .filter(v -> v.getDocument().getId().equals(document.getId()))
                .orElseThrow(() -> notFound("Version not found"));
        byte[] encrypted = storage.read(version.getEncryptedPath());
        try {
            SecretKey key = crypto.unwrapKey(keys.findById(document.getId()).orElseThrow(() -> notFound("Document key not found")).getWrappedKey());
            byte[] plaintext = crypto.decrypt(encrypted, key);
            try {
                audit.record(document.getOwner(), document, AuditAction.VERSION_VIEWED, "Version viewed");
                return plaintext;
            } catch (RuntimeException ex) {
                Arrays.fill(plaintext, (byte) 0);
                throw ex;
            }
        } finally {
            Arrays.fill(encrypted, (byte) 0);
        }
    }

    private void validateOverlays(EditRequest input) {
        if (input.overlays().size() > maxOverlays) throw violation(ErrorCode.VALIDATION_ERROR, "Too many overlays");
        for (OverlayRequest overlay : input.overlays()) {
            if (overlay.text().length() > maxTextLength || overlay.fontSize() < minFontSize || overlay.fontSize() > maxFontSize
                    || !Float.isFinite(overlay.x()) || !Float.isFinite(overlay.y()) || !Float.isFinite(overlay.fontSize())) {
                throw violation(ErrorCode.VALIDATION_ERROR, "Overlay values are outside configured bounds");
            }
        }
    }
    private Document ownedDocument(Long ownerId, UUID id) {
        return documents.findByIdAndOwnerIdAndDeletedFalse(id, ownerId).orElseThrow(() -> notFound("Document not found"));
    }
    private Document policyOrNull(String token) {
        try { return policies.resolveToken(token).getDocument(); } catch (RuntimeException ignored) { return null; }
    }
    private PolicyViolationException notFound(String message) { return violation(ErrorCode.NOT_FOUND, message); }
    private PolicyViolationException violation(ErrorCode code, String message) { return new PolicyViolationException(code, message); }
    private ActionRequestResponse toResponse(ActionRequest r) {
        return new ActionRequestResponse(r.getId(), r.getDocument().getId(), r.getPolicy().getId(), r.getType(),
                r.getRequestedBy().getId(), r.getStatus(), r.getRequestedAt(), r.getDecidedAt(), r.getReleasedAt(), r.getTransactionId());
    }
}
