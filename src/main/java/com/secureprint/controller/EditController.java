package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.service.EditService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requests")
public class EditController {
    private final EditService edits;
    public EditController(EditService edits) { this.edits=edits; }
    @PostMapping("/edit")
    @PreAuthorize("hasRole('PRINT_SHOP')")
    public ResponseEntity<ActionRequestResponse> submit(@AuthenticationPrincipal Long shopId, @Valid @RequestBody EditRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(edits.submit(shopId, request));
    }
}
