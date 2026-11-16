package com.secureprint.dto;

import com.secureprint.entity.PolicyAction;
import jakarta.validation.constraints.*;
import java.util.Set;

public record PolicyRequest(
        @NotEmpty Set<PolicyAction> allowedActions,
        Integer maxPrintCount,
        @Min(1) Integer maxEditCount,
        @NotNull @Min(1) Integer expiryMinutes) {
}
