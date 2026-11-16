package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.service.RequestService;
import com.secureprint.entity.RequestStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requests")
public class RequestController {
    private final RequestService requests;
    public RequestController(RequestService requests) { this.requests = requests; }

    @PostMapping
    @PreAuthorize("hasRole('PRINT_SHOP')")
    public ResponseEntity<ActionRequestResponse> create(@AuthenticationPrincipal Long shopId, @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requests.create(shopId, request));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINT_SHOP','USER')")
    public ActionRequestResponse get(@AuthenticationPrincipal Long actorId, @PathVariable UUID id) { return requests.get(actorId, id); }
    @GetMapping("/mine")
    @PreAuthorize("hasRole('PRINT_SHOP')")
    public PageResponse<ActionRequestResponse> mineFiltered(@AuthenticationPrincipal Long shopId,@RequestParam(required=false) RequestStatus status,
                                                              @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return requests.mine(shopId,status,page,size);
    }
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id, @Valid @RequestBody DecisionRequest decision) {
        requests.approve(ownerId, id, decision.note());
    }
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal Long ownerId, @PathVariable UUID id, @Valid @RequestBody DecisionRequest decision) {
        requests.reject(ownerId, id, decision.note());
    }
}
