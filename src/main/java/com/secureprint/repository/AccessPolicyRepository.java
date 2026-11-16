package com.secureprint.repository;
import com.secureprint.entity.AccessPolicy;
import com.secureprint.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, UUID> {
    void deleteAllByDocument(Document document);
    java.util.Optional<AccessPolicy> findFirstByDocumentAndRevokedFalseOrderByCreatedAtDesc(Document document);
    java.util.Optional<AccessPolicy> findByToken(String token);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update AccessPolicy p set p.revoked = true, p.token = null where p.document = :document")
    void revokeAllByDocument(@org.springframework.data.repository.query.Param("document") Document document);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE access_policies SET current_print_count = current_print_count + 1 WHERE id = :id AND revoked = false AND current_print_count < max_print_count AND expiry_timestamp > :now", nativeQuery = true)
    int consumePrint(@org.springframework.data.repository.query.Param("id") UUID id,
                     @org.springframework.data.repository.query.Param("now") java.time.Instant now);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE access_policies SET current_edit_count = current_edit_count + 1 WHERE id = :id AND revoked = false AND current_edit_count < max_edit_count AND expiry_timestamp > :now", nativeQuery = true)
    int consumeEdit(@org.springframework.data.repository.query.Param("id") UUID id,
                    @org.springframework.data.repository.query.Param("now") java.time.Instant now);
    @org.springframework.data.jpa.repository.Query("select count(p) from AccessPolicy p where p.revoked=false and p.expiryTimestamp > :now and p.expiryTimestamp <= :within and p.document.deleted=false")
    long countExpiring(java.time.Instant now, java.time.Instant within);
}
