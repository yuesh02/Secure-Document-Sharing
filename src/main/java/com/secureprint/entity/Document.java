package com.secureprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "documents")
public class Document {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(nullable = false) private String filename;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "uploaded_at", nullable = false) private Instant uploadedAt;
    @Column(nullable = false) private boolean deleted;
    @Column(name = "deleted_at") private Instant deletedAt;
    protected Document() {}
    public Document(UUID id, User owner, String filename, String contentType, long sizeBytes, Instant uploadedAt) {
        this.id = id; this.owner = owner; this.filename = filename; this.contentType = contentType;
        this.sizeBytes = sizeBytes; this.uploadedAt = uploadedAt; this.deleted = false;
    }
    public UUID getId() { return id; }
    public User getOwner() { return owner; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isDeleted() { return deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void markDeleted(Instant deletedAt) { this.deleted = true; this.deletedAt = deletedAt; }
}
