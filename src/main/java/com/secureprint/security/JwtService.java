package com.secureprint.security;
import com.secureprint.entity.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key; private final Duration lifetime;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.lifetime}") Duration lifetime) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.lifetime=lifetime;
    }
    public String issue(User user) {
        Date now=new Date(); Date expiry=new Date(now.getTime()+lifetime.toMillis());
        return Jwts.builder().subject(user.getId().toString()).claim("role",user.getRole().name()).issuedAt(now).expiration(expiry).signWith(key,Jwts.SIG.HS256).compact();
    }
    public Jws<Claims> parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token); }
    public long lifetimeSeconds() { return lifetime.toSeconds(); }
}
