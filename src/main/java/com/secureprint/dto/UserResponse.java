package com.secureprint.dto;

import com.secureprint.entity.Role;
import com.secureprint.entity.UserStatus;
import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, UserStatus status, Instant createdAt) {}
