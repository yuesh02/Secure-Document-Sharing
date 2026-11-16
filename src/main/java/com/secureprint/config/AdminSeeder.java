package com.secureprint.config;
import com.secureprint.entity.Role;
import com.secureprint.entity.User;
import com.secureprint.entity.UserStatus;
import com.secureprint.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {
    @Bean CommandLineRunner seedAdmin(UserRepository users,PasswordEncoder encoder,Clock clock,
                                      @Value("${app.admin-email}") String email,@Value("${app.admin-password}") String password) {
        return args -> {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return;
            }
            if (!users.existsByEmailIgnoreCase(email)) {
                users.save(new User("Administrator", email.toLowerCase(), encoder.encode(password),
                        Role.ADMIN, UserStatus.ACTIVE, Instant.now(clock)));
            }
        };
    }
}
