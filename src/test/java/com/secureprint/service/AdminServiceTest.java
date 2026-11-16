package com.secureprint.service;

import com.secureprint.entity.*;
import com.secureprint.exception.*;
import com.secureprint.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {
    @Test void adminCannotDisableThemselves() {
        UserRepository users=mock(UserRepository.class);
        AuditService audit=mock(AuditService.class);
        User admin=new User("Admin","a@example.com","hash",Role.ADMIN,UserStatus.ACTIVE,Instant.now());
        try {
            var field=User.class.getDeclaredField("id"); field.setAccessible(true); field.set(admin,1L);
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        AdminService service=new AdminService(users,audit,java.time.Clock.systemUTC());
        assertThrows(PolicyViolationException.class,()->service.updateStatus(1L,admin,UserStatus.DISABLED));
        verifyNoInteractions(users);
    }
}
