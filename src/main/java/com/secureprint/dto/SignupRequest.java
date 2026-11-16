package com.secureprint.dto;
import com.secureprint.entity.Role;
import jakarta.validation.constraints.*;
public record SignupRequest(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=320) String email,
                             @NotBlank @Size(min=8,max=100) String password, @NotNull Role role) {}
