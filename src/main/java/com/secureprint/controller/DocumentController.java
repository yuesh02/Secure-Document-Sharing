package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.service.DocumentService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@PreAuthorize("hasRole('USER')")
public class DocumentController {
    private final DocumentService documents;
    public DocumentController(DocumentService documents) { this.documents = documents; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal Long ownerId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documents.upload(ownerId, file));
    }
    @GetMapping
    public DocumentPageResponse list(@AuthenticationPrincipal Long ownerId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return documents.list(ownerId, page, size);
    }
    @GetMapping("/{id}")
    public DocumentResponse get(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id) { return documents.get(ownerId, id); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id) { documents.delete(ownerId, id); }
}
