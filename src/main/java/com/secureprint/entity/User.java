package com.secureprint.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private UserStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected User() {}
    public User(String name, String email, String passwordHash, Role role, UserStatus status, Instant createdAt) {
        this.name = name; this.email = email; this.passwordHash = passwordHash; this.role = role; this.status = status; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(UserStatus status) { this.status = status; }
}
