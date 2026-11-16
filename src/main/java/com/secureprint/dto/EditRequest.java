package com.secureprint.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record EditRequest(@NotBlank @Size(max=1000) String token,
                          @Size(max=1000) String changeNote,
                          @NotNull @Size(min=1) List<@Valid OverlayRequest> overlays) {}
