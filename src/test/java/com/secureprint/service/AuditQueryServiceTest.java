package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditQueryServiceTest {
    @Test void ownerQueryUsesDocumentAndNewestFirstRepositoryMethod() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        UUID documentId = UUID.randomUUID();
        when(documents.findByIdAndOwnerId(documentId, 7L)).thenReturn(java.util.Optional.of(mock(Document.class)));
        when(repository.findByDocumentIdOrderByTimestampDesc(eq(documentId), any())).thenReturn(new PageImpl<>(java.util.List.of()));
        var result = new AuditQueryService(repository, documents).forOwner(7L, documentId, 0, 20);
        assertEquals(0, result.totalElements());
        verify(repository).findByDocumentIdOrderByTimestampDesc(eq(documentId), any());
    }
}
