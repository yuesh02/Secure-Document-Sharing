package com.secureprint.dto;
import jakarta.validation.constraints.NotBlank;
public record TokenRequest(@NotBlank String token) {}
