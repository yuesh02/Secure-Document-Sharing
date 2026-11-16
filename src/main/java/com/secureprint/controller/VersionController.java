package com.secureprint.controller;

import com.secureprint.dto.VersionResponse;
import com.secureprint.service.EditService;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
@PreAuthorize("hasRole('USER')")
public class VersionController {
    private final EditService edits;
    public VersionController(EditService edits) { this.edits=edits; }
    @GetMapping("/{id}/versions")
    public List<VersionResponse> versions(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id) {
        return edits.versions(ownerId, id);
    }
    @GetMapping("/{id}/versions/{versionId}/content")
    public ResponseEntity<byte[]> content(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id, @PathVariable UUID versionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(edits.content(ownerId, id, versionId));
    }
}
