package com.secureprint.dto;
import com.secureprint.entity.RequestStatus;
import com.secureprint.entity.RequestType;
import java.time.Instant;
import java.util.UUID;
public record ActionRequestResponse(UUID id, UUID documentId, UUID policyId, RequestType type,
                                    Long requestedBy, RequestStatus status, Instant requestedAt,
                                    Instant decidedAt, Instant releasedAt, UUID transactionId) {}
