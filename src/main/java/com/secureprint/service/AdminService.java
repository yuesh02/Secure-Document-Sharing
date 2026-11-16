package com.secureprint.service;
import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users; private final AuditService audit; private final Clock clock;
    public AdminService(UserRepository users,AuditService audit,Clock clock){this.users=users;this.audit=audit;this.clock=clock;}
    @Transactional
    public void updateStatus(Long targetId,User actor,UserStatus status) {
        if (actor.getId().equals(targetId) && status == UserStatus.DISABLED) {
            throw new PolicyViolationException(ErrorCode.INVALID_STATE, "An administrator cannot disable themselves");
        }
        User target=users.findById(targetId).orElseThrow(()->new PolicyViolationException(ErrorCode.NOT_FOUND,"User not found"));
        target.setStatus(status); audit.record(actor,null,AuditAction.ADMIN_STATUS_CHANGE,"Status changed for user "+targetId+" to "+status);
    }
}
