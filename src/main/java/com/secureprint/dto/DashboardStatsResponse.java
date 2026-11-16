package com.secureprint.dto;

public record DashboardStatsResponse(long users, long userAccounts, long printShops, long admins,
                                     long activeDocuments, long deletedDocuments,
                                     long pendingRequests, long printsReleased,
                                     long rejectedAttemptsLast24Hours) {}
