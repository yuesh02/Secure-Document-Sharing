package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.entity.PolicyAction;
import com.secureprint.service.AccessService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/access")
@PreAuthorize("hasRole('PRINT_SHOP')")
public class PublicAccessController {
    private final AccessService access;
    public PublicAccessController(AccessService access) { this.access = access; }
    @PostMapping("/view")
    public ResponseEntity<byte[]> view(@AuthenticationPrincipal Long shopId, @Valid @RequestBody TokenRequest request) {
        return response(access.viewOrDownload(shopId, request.token(), PolicyAction.VIEW));
    }
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal Long shopId, @Valid @RequestBody TokenRequest request) {
        return response(access.viewOrDownload(shopId, request.token(), PolicyAction.DOWNLOAD));
    }
    private ResponseEntity<byte[]> response(ReleasedDocument result) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"").contentType(MediaType.APPLICATION_PDF).body(result.bytes());
    }
}
