package com.secureprint.controller;

import com.secureprint.dto.AuditPageResponse;
import com.secureprint.service.AuditQueryService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditController {
    private final AuditQueryService audits;
    public AuditController(AuditQueryService audits) { this.audits=audits; }
    @GetMapping("/documents/{id}/audit")
    @PreAuthorize("hasRole('USER')")
    public AuditPageResponse documentAudit(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id,
                                           @RequestParam(defaultValue="0") int page,
                                           @RequestParam(defaultValue="20") int size) {
        return audits.forOwner(ownerId, id, page, size);
    }
}
