package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.entity.PolicyAction;
import com.secureprint.service.AccessService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requests")
public class AccessController {
    private final AccessService access;
    public AccessController(AccessService access) { this.access = access; }
    @GetMapping("/{id}/document")
    @PreAuthorize("hasRole('PRINT_SHOP')")
    public ResponseEntity<byte[]> print(@AuthenticationPrincipal Long shopId, @PathVariable UUID id) {
        ReleasedDocument result = access.release(shopId, id);
        return pdf(result);
    }

    private ResponseEntity<byte[]> pdf(ReleasedDocument result) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"").contentType(MediaType.APPLICATION_PDF).body(result.bytes());
    }
}
