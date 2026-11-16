package com.secureprint.dto;
import java.util.List;
public record AuditPageResponse(List<AuditLogResponse> content, int page, int size,
                                long totalElements, int totalPages) {}
