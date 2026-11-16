package com.secureprint.dto;
import java.time.Instant;
import java.util.UUID;
public record DocumentResponse(UUID id, String filename, String contentType, long sizeBytes, Instant uploadedAt) {}
