package com.secureprint.repository;
import com.secureprint.entity.DocumentVersion;
import com.secureprint.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    java.util.List<DocumentVersion> findAllByDocument(Document document);
    void deleteAllByDocument(Document document);
    java.util.Optional<DocumentVersion> findByDocumentAndStatus(Document document, com.secureprint.entity.VersionStatus status);
    java.util.Optional<DocumentVersion> findTopByDocumentOrderByVersionNumberDesc(Document document);
}
