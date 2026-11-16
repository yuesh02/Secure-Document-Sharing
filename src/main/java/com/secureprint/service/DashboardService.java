package com.secureprint.service;

import com.secureprint.dto.*;
import com.secureprint.entity.*;
import com.secureprint.repository.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final DocumentRepository documents;
    private final ActionRequestRepository requests;
    private final Clock clock;
    public DashboardService(DocumentRepository documents,ActionRequestRepository requests,Clock clock){this.documents=documents;this.requests=requests;this.clock=clock;}
    @Transactional(readOnly=true)
    public DocumentSummaryResponse summary(Long ownerId) {
        Instant now=Instant.now(clock);
        return new DocumentSummaryResponse(documents.countActiveByOwner(ownerId),
                documents.countExpiringByOwner(ownerId,now,now.plus(24,java.time.temporal.ChronoUnit.HOURS)),
                requests.countByDocumentOwnerIdAndStatus(ownerId,RequestStatus.PENDING));
    }
    @Transactional(readOnly=true)
    public PageResponse<ActionRequestResponse> history(Long ownerId, RequestStatus status, int page, int size) {
        Page<ActionRequest> result=status==null
                ? requests.findByDocumentOwnerIdAndStatusNot(ownerId, RequestStatus.PENDING, pageable(page,size))
                : requests.findByDocumentOwnerIdAndStatus(ownerId,status,pageable(page,size));
        return PageResponse.of(result.map(this::toResponse));
    }
    private Pageable pageable(int page,int size){return PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100),Sort.by("requestedAt").descending());}
    private ActionRequestResponse toResponse(ActionRequest r){return new ActionRequestResponse(r.getId(),r.getDocument().getId(),r.getPolicy().getId(),r.getType(),r.getRequestedBy().getId(),r.getStatus(),r.getRequestedAt(),r.getDecidedAt(),r.getReleasedAt(),r.getTransactionId());}
}
