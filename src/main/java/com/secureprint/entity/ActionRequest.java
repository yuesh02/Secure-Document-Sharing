package com.secureprint.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="action_requests")
public class ActionRequest {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id",nullable=false) private Document document;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="policy_id",nullable=false) private AccessPolicy policy;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RequestType type;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="requested_by",nullable=false) private User requestedBy;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RequestStatus status;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pending_version_id") private DocumentVersion pendingVersion;
    @Column(name="transaction_id") private UUID transactionId;
    @Column(name="decision_note") private String decisionNote;
    @Column(name="requested_at",nullable=false) private Instant requestedAt;
    @Column(name="decided_at") private Instant decidedAt;
    @Column(name="released_at") private Instant releasedAt;
    protected ActionRequest() {}
    public ActionRequest(UUID id, Document document, AccessPolicy policy, RequestType type, User requestedBy, Instant requestedAt) {
        this.id=id; this.document=document; this.policy=policy; this.type=type; this.requestedBy=requestedBy;
        this.status=RequestStatus.PENDING; this.requestedAt=requestedAt;
    }
    public UUID getId(){return id;}
    public Document getDocument(){return document;}
    public AccessPolicy getPolicy(){return policy;}
    public RequestType getType(){return type;}
    public User getRequestedBy(){return requestedBy;}
    public RequestStatus getStatus(){return status;}
    public UUID getTransactionId(){return transactionId;}
    public Instant getRequestedAt(){return requestedAt;}
    public Instant getDecidedAt(){return decidedAt;}
    public Instant getReleasedAt(){return releasedAt;}
    public DocumentVersion getPendingVersion(){return pendingVersion;}
    public void setPendingVersion(DocumentVersion pendingVersion){this.pendingVersion=pendingVersion;}
    public void setStatus(RequestStatus status){this.status=status;}
    public void setDecisionNote(String note){this.decisionNote=note;}
    public void setDecidedAt(Instant value){this.decidedAt=value;}
    public void setTransactionId(UUID value){this.transactionId=value;}
    public void setReleasedAt(Instant value){this.releasedAt=value;}
}
