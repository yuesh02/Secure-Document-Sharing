package com.secureprint.controller;
import com.secureprint.dto.StatusUpdateRequest;
import com.secureprint.entity.*;
import com.secureprint.repository.UserRepository;
import com.secureprint.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.secureprint.dto.*;
import com.secureprint.service.AdminDashboardService;

@RestController @RequestMapping("/admin")
public class AdminController {
    private final AdminService admin; private final UserRepository users; private final AdminDashboardService dashboard;
    public AdminController(AdminService admin,UserRepository users,AdminDashboardService dashboard){this.admin=admin;this.users=users;this.dashboard=dashboard;}
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@PathVariable Long id,@Valid @RequestBody StatusUpdateRequest request,@AuthenticationPrincipal Long actorId) {
        User actor=users.findById(actorId).orElseThrow(); admin.updateStatus(id,actor,request.status());
    }
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> users(@RequestParam(required=false) Role role,@RequestParam(required=false) UserStatus status,
                                            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return dashboard.users(role,status,page,size);
    }
    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AdminDocumentResponse> documents(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return dashboard.documents(page,size);
    }
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardStatsResponse stats(){return dashboard.stats();}
}
