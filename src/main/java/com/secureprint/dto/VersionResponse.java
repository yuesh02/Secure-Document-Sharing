package com.secureprint.dto;

import com.secureprint.entity.VersionStatus;
import java.time.Instant;
import java.util.UUID;

public record VersionResponse(UUID id, int versionNumber, VersionStatus status,
                              String changeNote, Instant createdAt) {}
