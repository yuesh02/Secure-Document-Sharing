package com.secureprint.repository;
import com.secureprint.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.domain.*;
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByOwnerIdAndDeletedFalse(Long ownerId, Pageable pageable);
    java.util.Optional<Document> findByIdAndOwnerIdAndDeletedFalse(UUID id, Long ownerId);
    java.util.Optional<Document> findByIdAndOwnerId(UUID id, Long ownerId);
    Page<Document> findAllBy(Pageable pageable);
    long countByDeletedFalse();
    long countByDeletedTrue();
    @org.springframework.data.jpa.repository.Query("select count(d) from Document d where d.owner.id = :ownerId and d.deleted = false")
    long countActiveByOwner(Long ownerId);
    @org.springframework.data.jpa.repository.Query("select count(d) from Document d join AccessPolicy p on p.document=d where d.owner.id=:ownerId and d.deleted=false and p.revoked=false and p.expiryTimestamp > :now and p.expiryTimestamp <= :within")
    long countExpiringByOwner(Long ownerId, java.time.Instant now, java.time.Instant within);
    @org.springframework.data.jpa.repository.Query("""
        select distinct d from Document d join AccessPolicy p on p.document = d
        where d.deleted = false and p.revoked = false
          and (p.expiryTimestamp <= :now or (p.maxPrintCount is not null and p.currentPrintCount >= p.maxPrintCount))
        """)
    java.util.List<Document> findDocumentsForCleanup(java.time.Instant now);
}
