package com.secureprint.repository;
import com.secureprint.entity.ActionRequest;
import com.secureprint.entity.Document;
import com.secureprint.entity.AccessPolicy;
import com.secureprint.entity.User;
import com.secureprint.entity.RequestType;
import com.secureprint.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ActionRequestRepository extends JpaRepository<ActionRequest, UUID> {
    void deleteAllByDocument(Document document);
    boolean existsByPolicyAndRequestedByAndTypeAndStatus(AccessPolicy policy, User requestedBy, RequestType type, RequestStatus status);
    org.springframework.data.domain.Page<ActionRequest> findByRequestedById(Long userId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<ActionRequest> findByRequestedByIdAndStatus(Long userId, RequestStatus status, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<ActionRequest> findByDocumentOwnerIdAndStatusNot(Long ownerId, RequestStatus status, org.springframework.data.domain.Pageable pageable);
    long countByDocumentOwnerIdAndStatus(Long ownerId, RequestStatus status);
    long countByStatus(RequestStatus status);
    org.springframework.data.domain.Page<ActionRequest> findByDocumentOwnerIdAndStatus(Long ownerId, RequestStatus status, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE action_requests SET status = 'RELEASED', transaction_id = :transactionId, released_at = :releasedAt WHERE id = :id AND status = 'APPROVED'", nativeQuery = true)
    int claimApproved(@org.springframework.data.repository.query.Param("id") UUID id,
                      @org.springframework.data.repository.query.Param("transactionId") UUID transactionId,
                      @org.springframework.data.repository.query.Param("releasedAt") java.time.Instant releasedAt);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE action_requests SET status = :status, decision_note = :note, decided_at = :decidedAt WHERE id = :id AND status = 'PENDING'", nativeQuery = true)
    int decide(@org.springframework.data.repository.query.Param("id") UUID id,
               @org.springframework.data.repository.query.Param("status") String status,
               @org.springframework.data.repository.query.Param("note") String note,
               @org.springframework.data.repository.query.Param("decidedAt") java.time.Instant decidedAt);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE action_requests SET status = 'REJECTED', decision_note = :note, decided_at = :decidedAt WHERE document_id = :documentId AND status = 'PENDING'", nativeQuery = true)
    int rejectPendingForDocument(@org.springframework.data.repository.query.Param("documentId") UUID documentId,
                                 @org.springframework.data.repository.query.Param("note") String note,
                                 @org.springframework.data.repository.query.Param("decidedAt") java.time.Instant decidedAt);
    boolean existsByDocumentAndTypeAndStatus(Document document, RequestType type, RequestStatus status);
}
