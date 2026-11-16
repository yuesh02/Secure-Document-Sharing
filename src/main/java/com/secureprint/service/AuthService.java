package com.secureprint.service;
import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.UserRepository;
import com.secureprint.security.JwtService;
import java.time.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final AuditService audit; private final Clock clock;
    public AuthService(UserRepository users,PasswordEncoder encoder,JwtService jwt,AuditService audit,Clock clock){this.users=users;this.encoder=encoder;this.jwt=jwt;this.audit=audit;this.clock=clock;}
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if(request.role()==Role.ADMIN) throw new PolicyViolationException(ErrorCode.FORBIDDEN,"ADMIN signup is not allowed");
        if(users.existsByEmailIgnoreCase(request.email())) throw new ConflictException("Email is already registered");
        User user=new User(request.name(),request.email().toLowerCase(),encoder.encode(request.password()),request.role(),
                request.role()==Role.USER?UserStatus.ACTIVE:UserStatus.PENDING_APPROVAL,Instant.now(clock));
        User saved = users.save(user);
        audit.record(saved, null, AuditAction.SIGNUP, "Account created");
        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(),
                saved.getStatus(), saved.getCreatedAt());
    }
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user=users.findByEmailIgnoreCase(request.email()).orElse(null);
        if(user==null || !encoder.matches(request.password(),user.getPasswordHash()) || user.getStatus()!=UserStatus.ACTIVE) {
            audit.recordRejected(null,null,AuditAction.LOGIN_FAILED,"Invalid credentials or inactive account");
            throw new UnauthorizedException("Authentication failed");
        }
        audit.record(user,null,AuditAction.LOGIN_SUCCESS,"Login successful");
        return new AuthResponse(jwt.issue(user),jwt.lifetimeSeconds());
    }
}
