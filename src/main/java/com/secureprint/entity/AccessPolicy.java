package com.secureprint.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
@Entity @Table(name="access_policies")
public class AccessPolicy {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id",nullable=false) private Document document;
    @Column(unique=true) private String token;
    @Column(name="max_print_count") private Integer maxPrintCount;
    @Column(nullable=false) private int currentPrintCount;
    @Column(name="max_edit_count") private Integer maxEditCount;
    @Column(nullable=false) private int currentEditCount;
    @Column(name="expiry_timestamp",nullable=false) private Instant expiryTimestamp;
    @Column(nullable=false) private boolean revoked;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @Column(nullable=false) private Instant createdAt;
    @ElementCollection
    @CollectionTable(name="access_policy_actions", joinColumns=@JoinColumn(name="policy_id"))
    @Column(name="action", nullable=false)
    @Enumerated(EnumType.STRING)
    private Set<PolicyAction> actions = new HashSet<>();
    protected AccessPolicy() {}
    public AccessPolicy(UUID id, Document document, String token, Integer maxPrintCount, Integer maxEditCount,
                        Instant expiryTimestamp, User createdBy, Instant createdAt, Set<PolicyAction> actions) {
        this.id=id; this.document=document; this.token=token; this.maxPrintCount=maxPrintCount;
        this.maxEditCount=maxEditCount; this.expiryTimestamp=expiryTimestamp; this.createdBy=createdBy;
        this.createdAt=createdAt; this.actions=new HashSet<>(actions); this.currentPrintCount=0; this.currentEditCount=0; this.revoked=false;
    }
    public UUID getId() { return id; }
    public Document getDocument() { return document; }
    public String getToken() { return token; }
    public Integer getMaxPrintCount() { return maxPrintCount; }
    public int getCurrentPrintCount() { return currentPrintCount; }
    public Integer getMaxEditCount() { return maxEditCount; }
    public int getCurrentEditCount() { return currentEditCount; }
    public Instant getExpiryTimestamp() { return expiryTimestamp; }
    public boolean isRevoked() { return revoked; }
    public Set<PolicyAction> getActions() { return Set.copyOf(actions); }
    public void revoke() { this.revoked=true; this.token=null; }
}
