package com.secureprint.dto;

import jakarta.validation.constraints.*;

public record OverlayRequest(
        @NotNull @Min(1) Integer page,
        @NotNull @PositiveOrZero Float x,
        @NotNull @PositiveOrZero Float y,
        @NotBlank @Size(max=500) String text,
        @NotNull @DecimalMin("1.0") @DecimalMax("200.0") Float fontSize) {}
