package com.secureprint.dto;
import java.util.UUID;
public record ReleasedDocument(UUID transactionId, byte[] bytes) {}
