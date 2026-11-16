package com.secureprint.dto;

public record DocumentSummaryResponse(long activeDocuments, long expiringWithin24Hours,
                                      long pendingRequestCount) {}
