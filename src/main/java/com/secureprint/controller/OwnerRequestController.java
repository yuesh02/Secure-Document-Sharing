package com.secureprint.controller;

import com.secureprint.dto.ActionRequestPageResponse;
import com.secureprint.service.RequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owners/me")
@PreAuthorize("hasRole('USER')")
public class OwnerRequestController {
    private final RequestService requests;
    public OwnerRequestController(RequestService requests) { this.requests = requests; }
    @GetMapping("/pending-requests")
    public ActionRequestPageResponse pending(@AuthenticationPrincipal Long ownerId,
                                             @RequestParam(defaultValue="0") int page,
                                             @RequestParam(defaultValue="20") int size) {
        return requests.pendingForOwner(ownerId, page, size);
    }
}
