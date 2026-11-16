package com.secureprint.controller;

import com.secureprint.dto.*;
import com.secureprint.entity.RequestStatus;
import com.secureprint.service.DashboardService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('USER')")
public class OwnerDashboardController {
    private final DashboardService dashboard;
    public OwnerDashboardController(DashboardService dashboard){this.dashboard=dashboard;}
    @GetMapping("/documents/summary")
    public DocumentSummaryResponse summary(@AuthenticationPrincipal Long ownerId){return dashboard.summary(ownerId);}
    @GetMapping("/owners/me/requests")
    public PageResponse<ActionRequestResponse> history(@AuthenticationPrincipal Long ownerId,@RequestParam(required=false) RequestStatus status,
                                                        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return dashboard.history(ownerId,status,page,size);
    }
}
