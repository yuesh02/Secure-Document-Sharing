package com.secureprint.dto;
public record AuthResponse(String token, long expiresInSeconds) {}
