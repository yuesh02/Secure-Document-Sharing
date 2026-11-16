package com.secureprint.security;

import com.secureprint.entity.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

class JwtServiceTest {
    @Test void tokenContainsUserIdAndRole() throws Exception {
        JwtService service=new JwtService("12345678901234567890123456789012",Duration.ofHours(1));
        User user=new User("Alice","a@example.com","hash",Role.USER,UserStatus.ACTIVE,Instant.now());
        Field id=User.class.getDeclaredField("id"); id.setAccessible(true); id.set(user,42L);
        String token=service.issue(user);
        assertEquals("USER",service.parse(token).getPayload().get("role"));
    }
    @Test void wrongSignatureIsRejected() throws Exception {
        JwtService service=new JwtService("12345678901234567890123456789012",Duration.ofHours(1));
        JwtService other=new JwtService("abcdefghijklmnopqrstuvwxyz123456",Duration.ofHours(1));
        User user=new User("Alice","a@example.com","hash",Role.USER,UserStatus.ACTIVE,Instant.now());
        Field id=User.class.getDeclaredField("id"); id.setAccessible(true); id.set(user,42L);
        assertThrows(RuntimeException.class,()->other.parse(service.issue(user)));
    }
}
