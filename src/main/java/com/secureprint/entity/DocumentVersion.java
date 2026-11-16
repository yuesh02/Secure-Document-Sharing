package com.secureprint.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="document_versions", uniqueConstraints=@UniqueConstraint(columnNames={"document_id","version_number"}))
public class DocumentVersion {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id", nullable=false) private Document document;
    @Column(name="version_number", nullable=false) private int versionNumber;
    @Column(name="encrypted_path", nullable=false) private String encryptedPath;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private VersionStatus status;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="edited_by") private User editedBy;
    @Column(name="change_note") private String changeNote;
    @Column(nullable=false) private Instant createdAt;
    protected DocumentVersion() {}
    public DocumentVersion(UUID id, Document document, int versionNumber, String encryptedPath, VersionStatus status, Instant createdAt) {
        this.id = id; this.document = document; this.versionNumber = versionNumber; this.encryptedPath = encryptedPath;
        this.status = status; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getEncryptedPath() { return encryptedPath; }
    public VersionStatus getStatus() { return status; }
    public Document getDocument() { return document; }
    public int getVersionNumber() { return versionNumber; }
    public String getChangeNote() { return changeNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(VersionStatus status) { this.status = status; }
    public void setEditedBy(User editedBy) { this.editedBy = editedBy; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }
}
