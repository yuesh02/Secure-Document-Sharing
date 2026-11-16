package com.secureprint.dto;

import com.secureprint.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminDocumentResponse(UUID id, Long ownerId, String filename, String contentType,
                                    long sizeBytes, Instant uploadedAt, boolean deleted, Instant deletedAt) {}
