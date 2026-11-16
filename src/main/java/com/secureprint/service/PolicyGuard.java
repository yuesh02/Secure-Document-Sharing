package com.secureprint.service;

import com.secureprint.entity.AccessPolicy;
import com.secureprint.entity.PolicyAction;
import com.secureprint.exception.ErrorCode;
import com.secureprint.exception.PolicyViolationException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class PolicyGuard {
    private final Clock clock;

    public PolicyGuard(Clock clock) {
        this.clock = clock;
    }

    public void check(AccessPolicy policy, PolicyAction action) {
        if (!policy.getActions().contains(action)) {
            throw new PolicyViolationException(ErrorCode.ACTION_NOT_ALLOWED, "Requested action is not allowed");
        }
        if (!Instant.now(clock).isBefore(policy.getExpiryTimestamp())) {
            throw new PolicyViolationException(ErrorCode.ACCESS_EXPIRED, "Access policy has expired");
        }
        if (action == PolicyAction.PRINT && policy.getMaxPrintCount() != null
                && policy.getCurrentPrintCount() >= policy.getMaxPrintCount()) {
            throw new PolicyViolationException(ErrorCode.PRINT_LIMIT_REACHED, "Print limit has been reached");
        }
        if (action == PolicyAction.EDIT && policy.getMaxEditCount() != null
                && policy.getCurrentEditCount() >= policy.getMaxEditCount()) {
            throw new PolicyViolationException(ErrorCode.EDIT_LIMIT_REACHED, "Edit limit has been reached");
        }
    }
}
