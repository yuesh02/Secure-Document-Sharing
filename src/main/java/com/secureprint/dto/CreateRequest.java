package com.secureprint.dto;
import com.secureprint.entity.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CreateRequest(@NotBlank String token, @NotNull RequestType type) {}
