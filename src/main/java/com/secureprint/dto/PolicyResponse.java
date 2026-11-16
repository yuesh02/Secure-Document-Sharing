package com.secureprint.dto;

import com.secureprint.entity.PolicyAction;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PolicyResponse(UUID id, Set<PolicyAction> allowedActions, String token,
                             Integer maxPrintCount, int currentPrintCount,
                             Integer maxEditCount, int currentEditCount,
                             Instant expiryTimestamp, boolean revoked) {
}
