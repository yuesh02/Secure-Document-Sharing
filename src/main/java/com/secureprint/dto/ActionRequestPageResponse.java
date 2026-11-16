package com.secureprint.dto;
import java.util.List;
public record ActionRequestPageResponse(List<ActionRequestResponse> content, int page, int size,
                                        long totalElements, int totalPages) {}
