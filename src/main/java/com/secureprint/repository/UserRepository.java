package com.secureprint.repository;
import com.secureprint.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import com.secureprint.entity.*;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findAllBy(Pageable pageable);
    long countByRole(Role role);
    long countByStatus(UserStatus status);
}
