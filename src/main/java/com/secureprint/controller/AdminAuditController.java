package com.secureprint.controller;

import com.secureprint.dto.AuditPageResponse;
import com.secureprint.entity.*;
import com.secureprint.service.AuditQueryService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AuditQueryService audits;
    public AdminAuditController(AuditQueryService audits) { this.audits=audits; }
    @GetMapping
    public AuditPageResponse search(@RequestParam(required=false) Long actorId,
                                    @RequestParam(required=false) UUID documentId,
                                    @RequestParam(required=false) AuditAction action,
                                    @RequestParam(required=false) AuditStatus status,
                                    @RequestParam(required=false) Instant from,
                                    @RequestParam(required=false) Instant to,
                                    @RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="20") int size) {
        return audits.search(actorId, documentId, action, status, from, to, page, size);
    }
}
