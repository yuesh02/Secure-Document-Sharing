package com.secureprint.dto;
import com.secureprint.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
public record StatusUpdateRequest(@NotNull UserStatus status) {}
