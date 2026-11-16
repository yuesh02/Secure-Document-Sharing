package com.secureprint.controller;
import com.secureprint.dto.*;
import com.secureprint.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth){this.auth=auth;}
    @PostMapping("/signup")
    ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.signup(request));
    }
    @PostMapping("/login") AuthResponse login(@Valid @RequestBody LoginRequest request){return auth.login(request);}
}
