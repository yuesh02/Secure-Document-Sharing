package com.secureprint.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;
@Entity @Table(name="document_keys")
public class DocumentKey implements Persistable<UUID> {
    @Id @Column(name="document_id") private UUID documentId;
    @OneToOne @MapsId @JoinColumn(name="document_id") private Document document;
    @Column(name="wrapped_key", nullable=false) private byte[] wrappedKey;
    @Column(nullable=false) private Instant createdAt;
    @Transient private boolean newRecord;
    protected DocumentKey() {}
    public DocumentKey(Document document, byte[] wrappedKey, Instant createdAt) {
        this.document = document; this.documentId = document.getId(); this.wrappedKey = wrappedKey;
        this.createdAt = createdAt; this.newRecord = true;
    }
    @Override public UUID getId() { return documentId; }
    @Override public boolean isNew() { return newRecord; }
    @PostLoad @PostPersist void markNotNew() { this.newRecord = false; }
    public UUID getDocumentId() { return documentId; }
    public byte[] getWrappedKey() { return wrappedKey.clone(); }
}
