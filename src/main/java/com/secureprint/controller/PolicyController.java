package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.service.PolicyService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('USER')")
public class PolicyController {
    private final PolicyService policies;

    public PolicyController(PolicyService policies) {
        this.policies = policies;
    }

    @PostMapping("/documents/{id}/policy")
    public ResponseEntity<PolicyResponse> create(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id,
                                                  @Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policies.create(ownerId, id, request));
    }

    @GetMapping("/documents/{id}/policy")
    public PolicyResponse get(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id) {
        return policies.get(ownerId, id);
    }

    @GetMapping(value = "/policies/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id) {
        return policies.qr(ownerId, id);
    }

    @PostMapping("/policies/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id) {
        policies.revoke(ownerId, id);
    }
}
