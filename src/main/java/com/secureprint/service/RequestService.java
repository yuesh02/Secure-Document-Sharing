package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {
    private final ActionRequestRepository requests;
    private final PolicyService policies;
    private final UserRepository users;
    private final AuditService audit;
    private final Clock clock;
    private final PolicyGuard guard;
    private final AccessPolicyRepository policyRepository;
    private final DocumentVersionRepository versionRepository;
    private final FileStorageService storage;

    public RequestService(ActionRequestRepository requests, PolicyService policies, UserRepository users,
                          AuditService audit, Clock clock, PolicyGuard guard,
                          AccessPolicyRepository policyRepository, DocumentVersionRepository versionRepository,
                          FileStorageService storage) {
        this.requests=requests; this.policies=policies; this.users=users; this.audit=audit; this.clock=clock; this.guard=guard;
        this.policyRepository=policyRepository; this.versionRepository=versionRepository; this.storage=storage;
    }

    @Transactional
    public ActionRequestResponse create(Long shopId, CreateRequest input) {
        User shop = users.findById(shopId).orElseThrow(() -> notFound());
        AccessPolicy policy;
        try {
            policy = policies.resolveToken(input.token());
            if (input.type() != RequestType.PRINT) throw violation(ErrorCode.ACTION_NOT_ALLOWED, "Only PRINT requests are supported");
            guard.check(policy, PolicyAction.PRINT);
            if (requests.existsByPolicyAndRequestedByAndTypeAndStatus(policy, shop, RequestType.PRINT, RequestStatus.PENDING)) {
                throw violation(ErrorCode.INVALID_STATE, "A pending request already exists");
            }
        } catch (PolicyViolationException ex) {
            reject(shop, policyOrNull(input.token()), ex.getMessage());
            throw ex;
        }
        ActionRequest request = new ActionRequest(UUID.randomUUID(), policy.getDocument(), policy, RequestType.PRINT, shop, Instant.now(clock));
        requests.save(request);
        audit.record(shop, policy.getDocument(), AuditAction.REQUEST_CREATED, "PRINT request created");
        return toResponse(request);
    }

    @Transactional(readOnly=true)
    public ActionRequestResponse get(Long actorId, UUID id) {
        ActionRequest request = requests.findById(id).orElseThrow(this::notFound);
        if (!request.getRequestedBy().getId().equals(actorId)
                && !request.getDocument().getOwner().getId().equals(actorId)) throw notFound();
        return toResponse(request);
    }

    @Transactional(readOnly=true)
    public ActionRequestPageResponse mine(Long shopId, int page, int size) {
        Page<ActionRequest> result = requests.findByRequestedById(shopId, pageable(page,size));
        return page(result);
    }
    @Transactional(readOnly=true)
    public PageResponse<ActionRequestResponse> mine(Long shopId, RequestStatus status, int page, int size) {
        Page<ActionRequest> result=status==null?requests.findByRequestedById(shopId,pageable(page,size))
                :requests.findByRequestedByIdAndStatus(shopId,status,pageable(page,size));
        return PageResponse.of(result.map(this::toResponse));
    }

    @Transactional(readOnly=true)
    public ActionRequestPageResponse pendingForOwner(Long ownerId, int page, int size) {
        Page<ActionRequest> result = requests.findByDocumentOwnerIdAndStatus(ownerId, RequestStatus.PENDING, pageable(page,size));
        return page(result);
    }

    @Transactional
    public void approve(Long ownerId, UUID id, String note) {
        ActionRequest request = ownerRequest(ownerId, id);
        if (request.getType() == RequestType.EDIT) {
            approveEdit(request, note);
            return;
        }
        int changed = requests.decide(id, RequestStatus.APPROVED.name(), note, Instant.now(clock));
        if (changed != 1) {
            reject(request.getDocument().getOwner(), request.getDocument(), "Request is no longer pending");
            throw violation(ErrorCode.INVALID_STATE, "Request is no longer pending");
        }
        audit.record(request.getDocument().getOwner(), request.getDocument(), AuditAction.REQUEST_APPROVED, "Request approved");
    }

    @Transactional
    public void reject(Long ownerId, UUID id, String note) {
        ActionRequest request = ownerRequest(ownerId, id);
        if (request.getType() == RequestType.EDIT) {
            rejectEdit(request, note);
            return;
        }
        int changed = requests.decide(id, RequestStatus.REJECTED.name(), note, Instant.now(clock));
        if (changed != 1) {
            reject(request.getDocument().getOwner(), request.getDocument(), "Request is no longer pending");
            throw violation(ErrorCode.INVALID_STATE, "Request is no longer pending");
        }
        audit.record(request.getDocument().getOwner(), request.getDocument(), AuditAction.REQUEST_REJECTED, "Request rejected");
    }

    private void approveEdit(ActionRequest request, String note) {
        Instant now = Instant.now(clock);
        if (policyRepository.consumeEdit(request.getPolicy().getId(), now) != 1) {
            AccessPolicy policy = policyRepository.findById(request.getPolicy().getId()).orElseThrow(this::notFound);
            ErrorCode code = !now.isBefore(policy.getExpiryTimestamp()) ? ErrorCode.ACCESS_EXPIRED : ErrorCode.EDIT_LIMIT_REACHED;
            audit.recordRejected(request.getDocument().getOwner(), request.getDocument(), AuditAction.EDIT_APPROVED, code.name());
            throw violation(code, code == ErrorCode.ACCESS_EXPIRED ? "Access policy has expired" : "Edit limit has been reached");
        }
        DocumentVersion active = versionRepository.findByDocumentAndStatus(request.getDocument(), VersionStatus.ACTIVE)
                .orElseThrow(() -> violation(ErrorCode.INVALID_STATE, "Active version not found"));
        DocumentVersion pending = request.getPendingVersion();
        if (pending == null || pending.getStatus() != VersionStatus.PENDING) {
            throw violation(ErrorCode.INVALID_STATE, "Pending edit version is invalid");
        }
        active.setStatus(VersionStatus.ARCHIVED);
        pending.setStatus(VersionStatus.ACTIVE);
        versionRepository.save(active);
        versionRepository.save(pending);
        int changed = requests.decide(request.getId(), RequestStatus.APPROVED.name(), note, now);
        if (changed != 1) throw violation(ErrorCode.INVALID_STATE, "Request is no longer pending");
        audit.record(request.getDocument().getOwner(), request.getDocument(), AuditAction.EDIT_APPROVED, "Edit approved");
    }

    private void rejectEdit(ActionRequest request, String note) {
        int changed = requests.decide(request.getId(), RequestStatus.REJECTED.name(), note, Instant.now(clock));
        if (changed != 1) throw violation(ErrorCode.INVALID_STATE, "Request is no longer pending");
        DocumentVersion pending = request.getPendingVersion();
        if (pending != null) {
            storage.delete(pending.getEncryptedPath());
            pending.setStatus(VersionStatus.REJECTED);
            versionRepository.save(pending);
        }
        audit.record(request.getDocument().getOwner(), request.getDocument(), AuditAction.EDIT_REJECTED, "Edit rejected");
    }

    private ActionRequest ownerRequest(Long ownerId, UUID id) {
        ActionRequest request = requests.findById(id).orElseThrow(this::notFound);
        if (!request.getDocument().getOwner().getId().equals(ownerId)) {
            User actor = users.findById(ownerId).orElse(null);
            reject(actor, request.getDocument(), "Request does not belong to owner");
            throw notFound();
        }
        return request;
    }

    private PolicyViolationException notFound() { return violation(ErrorCode.NOT_FOUND, "Request not found"); }
    private PolicyViolationException violation(ErrorCode code, String message) { return new PolicyViolationException(code, message); }
    private void reject(User actor, Document document, String reason) { audit.recordRejected(actor, document, AuditAction.REQUEST_REJECTED, reason); }
    private Document documentOrNull(AccessPolicy policy) { return policy == null ? null : policy.getDocument(); }
    private Document policyOrNull(String token) {
        try { return documentOrNull(policies.resolveToken(token)); } catch (RuntimeException ignored) { return null; }
    }
    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size),100), Sort.by("requestedAt").descending());
    }
    private ActionRequestPageResponse page(Page<ActionRequest> result) {
        return new ActionRequestPageResponse(result.map(this::toResponse).getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    private ActionRequestResponse toResponse(ActionRequest r) {
        return new ActionRequestResponse(r.getId(), r.getDocument().getId(), r.getPolicy().getId(), r.getType(),
                r.getRequestedBy().getId(), r.getStatus(), r.getRequestedAt(), r.getDecidedAt(), r.getReleasedAt(), r.getTransactionId());
    }
}
