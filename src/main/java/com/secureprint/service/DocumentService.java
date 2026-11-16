package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private final DocumentRepository documents;
    private final DocumentKeyRepository keys;
    private final DocumentVersionRepository versions;
    private final AccessPolicyRepository policies;
    private final ActionRequestRepository requests;
    private final UserRepository users;
    private final CryptoService crypto;
    private final FileStorageService storage;
    private final AuditService audit;
    private final Clock clock;
    private final long maxBytes;

    public DocumentService(DocumentRepository documents, DocumentKeyRepository keys, DocumentVersionRepository versions,
                           AccessPolicyRepository policies, ActionRequestRepository requests, UserRepository users,
                           CryptoService crypto, FileStorageService storage, AuditService audit, Clock clock,
                           @Value("${app.upload-max-bytes}") long maxBytes) {
        this.documents = documents; this.keys = keys; this.versions = versions; this.policies = policies;
        this.requests = requests; this.users = users; this.crypto = crypto; this.storage = storage;
        this.audit = audit; this.clock = clock; this.maxBytes = maxBytes;
    }

    @Transactional
    public DocumentResponse upload(Long ownerId, MultipartFile file) {
        byte[] plaintext = readAndValidate(file);
        SecretKey documentKey = crypto.generateKey();
        String path = storage.store(crypto.encrypt(plaintext, documentKey));
        try {
            User owner = users.findById(ownerId).orElseThrow(() -> new PolicyViolationException(ErrorCode.NOT_FOUND, "Owner not found"));
            Instant now = Instant.now(clock);
            Document document = new Document(UUID.randomUUID(), owner, safeFilename(file.getOriginalFilename()), file.getContentType(), plaintext.length, now);
            Document persistedDocument = documents.save(document);
            keys.save(new DocumentKey(persistedDocument, crypto.wrapKey(documentKey), now));
            versions.save(new DocumentVersion(UUID.randomUUID(), persistedDocument, 1, path, VersionStatus.ACTIVE, now));
            audit.record(owner, persistedDocument, AuditAction.UPLOAD, "Document uploaded");
            return toResponse(persistedDocument);
        } catch (RuntimeException ex) {
            storage.delete(path);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public DocumentPageResponse list(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("uploadedAt").descending());
        Page<Document> result = documents.findByOwnerIdAndDeletedFalse(ownerId, pageable);
        return new DocumentPageResponse(result.map(this::toResponse).getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long ownerId, UUID documentId) {
        return toResponse(findOwned(ownerId, documentId));
    }

    @Transactional
    public void delete(Long ownerId, UUID documentId) {
        Document document = findOwned(ownerId, documentId);
        try {
            for (DocumentVersion version : versions.findAllByDocument(document)) storage.delete(version.getEncryptedPath());
            requests.deleteAllByDocument(document);
            policies.revokeAllByDocument(document);
            versions.deleteAllByDocument(document);
            keys.deleteById(document.getId());
            document.markDeleted(Instant.now(clock));
            documents.save(document);
            audit.record(document.getOwner(), document, AuditAction.DOCUMENT_DELETED, "Document deleted");
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    private Document findOwned(Long ownerId, UUID documentId) {
        return documents.findByIdAndOwnerIdAndDeletedFalse(documentId, ownerId)
                .orElseThrow(() -> new PolicyViolationException(ErrorCode.NOT_FOUND, "Document not found"));
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "PDF file is missing or exceeds the size limit");
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "Only application/pdf files are accepted");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') {
                throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "File is not a valid PDF");
            }
            try (PDDocument ignored = Loader.loadPDF(bytes)) { return bytes; }
        } catch (PolicyViolationException ex) {
            throw ex;
        } catch (IOException | IllegalArgumentException ex) {
            throw new PolicyViolationException(ErrorCode.VALIDATION_ERROR, "File is not a valid PDF");
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "document.pdf";
        String normalized = java.nio.file.Paths.get(filename).getFileName().toString();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }
    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getFilename(), document.getContentType(), document.getSizeBytes(), document.getUploadedAt());
    }
}
