package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.*;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentKeyRepository keys = mock(DocumentKeyRepository.class);
    private final DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
    private final AccessPolicyRepository policies = mock(AccessPolicyRepository.class);
    private final ActionRequestRepository requests = mock(ActionRequestRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final CryptoService crypto = new CryptoService(Base64.getEncoder().encodeToString(new byte[32]));
    private final FileStorageService storage;
    private final java.nio.file.Path storageDirectory;
    private final DocumentService service;
    private final User owner = new User("Owner", "owner@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());

    DocumentServiceTest() throws Exception {
        storageDirectory = Files.createTempDirectory("document-service");
        storage = new FileStorageService(storageDirectory.toString());
        service = new DocumentService(documents, keys, versions, policies, requests, users, crypto, storage, audit,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), 1_000_000);
    }

    @Test void rejectsSpoofedContentTypeAndNonPdf() {
        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "not a pdf".getBytes());
        assertThrows(PolicyViolationException.class, () -> service.upload(1L, file));
        MultipartFile spoofed = new MockMultipartFile("file", "x.txt", "text/plain", pdfBytes());
        assertThrows(PolicyViolationException.class, () -> service.upload(1L, spoofed));
    }

    @Test void rejectsOversizedFile() {
        DocumentService limited = new DocumentService(documents, keys, versions, policies, requests, users, crypto, storage, audit,
                Clock.systemUTC(), 3);
        assertThrows(PolicyViolationException.class, () -> limited.upload(1L,
                new MockMultipartFile("file", "x.pdf", "application/pdf", pdfBytes())));
    }

    @Test void anotherOwnersDocumentIsNotFound() {
        UUID id = UUID.randomUUID();
        when(documents.findByIdAndOwnerIdAndDeletedFalse(id, 1L)).thenReturn(Optional.empty());
        assertThrows(PolicyViolationException.class, () -> service.get(1L, id));
    }

    @Test void databaseFailureDeletesStoredBlob() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        doThrow(new RuntimeException("database failure")).when(documents).save(ArgumentMatchers.any(Document.class));
        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", pdfBytes());
        assertThrows(RuntimeException.class, () -> service.upload(1L, file));
        verify(documents).save(any(Document.class));
        verify(keys, never()).save(any());
        try (var files = Files.list(storageDirectory)) {
            assertEquals(0, files.count());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private byte[] pdfBytes() {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());
            pdf.save(output);
            return output.toByteArray();
        } catch (Exception ex) { throw new AssertionError(ex); }
    }
}
