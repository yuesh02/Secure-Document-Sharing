package com.secureprint.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="audit_logs")
@org.hibernate.annotations.Immutable
public class AuditLog {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id") private User actor;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id") private Document document;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private AuditAction action;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private AuditStatus status;
    @Column(nullable=false,length=2000) private String details;
    @Column(name="timestamp",nullable=false) private Instant timestamp;
    protected AuditLog() {}
    public AuditLog(User actor, Document document, AuditAction action, AuditStatus status, String details, Instant timestamp) {
        this.id=UUID.randomUUID(); this.actor=actor; this.document=document; this.action=action; this.status=status; this.details=details; this.timestamp=timestamp;
    }
    public UUID getId(){return id;}
    public User getActor(){return actor;}
    public Document getDocument(){return document;}
    public AuditAction getAction(){return action;}
    public AuditStatus getStatus(){return status;}
    public String getDetails(){return details;}
    public Instant getTimestamp(){return timestamp;}
}
