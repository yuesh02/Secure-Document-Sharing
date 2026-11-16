package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.exception.UnauthorizedException;
import com.secureprint.repository.UserRepository;
import com.secureprint.security.JwtService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users=mock(UserRepository.class);
    private final AuditService audit=mock(AuditService.class);
    private final JwtService jwt=mock(JwtService.class);
    private final AuthService service=new AuthService(users,new BCryptPasswordEncoder(),jwt,audit,Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),ZoneOffset.UTC));

    @Test void signupUserIsActiveAndAudited() {
        when(users.existsByEmailIgnoreCase("a@example.com")).thenReturn(false);
        User saved = new User("Alice", "a@example.com", "hash", Role.USER, UserStatus.ACTIVE, Instant.now());
        when(users.save(any(User.class))).thenReturn(saved);
        UserResponse response = service.signup(new SignupRequest("Alice","a@example.com","password8",Role.USER));
        verify(users).save(ArgumentMatchers.argThat(u -> u.getStatus()==UserStatus.ACTIVE && u.getRole()==Role.USER));
        verify(audit).record(any(),isNull(),eq(AuditAction.SIGNUP),anyString());
        assertEquals("a@example.com", response.email());
    }
    @Test void printShopStartsPending() {
        when(users.existsByEmailIgnoreCase("shop@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.signup(new SignupRequest("Shop","shop@example.com","password8",Role.PRINT_SHOP));
        verify(users).save(ArgumentMatchers.argThat(u -> u.getStatus()==UserStatus.PENDING_APPROVAL));
    }
    @Test void inactiveAccountCannotLoginAndRejectedAuditIsRecorded() {
        User user=new User("Shop","shop@example.com",new BCryptPasswordEncoder().encode("password8"),Role.PRINT_SHOP,UserStatus.PENDING_APPROVAL,Instant.now());
        when(users.findByEmailIgnoreCase("shop@example.com")).thenReturn(Optional.of(user));
        assertThrows(UnauthorizedException.class,()->service.login(new LoginRequest("shop@example.com","password8")));
        verify(audit).recordRejected(isNull(),isNull(),eq(AuditAction.LOGIN_FAILED),anyString());
    }
}
