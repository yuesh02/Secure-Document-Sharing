package com.secureprint.security;
import com.secureprint.entity.User;
import com.secureprint.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt; private final UserRepository users;
    public JwtAuthFilter(JwtService jwt,UserRepository users){this.jwt=jwt;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ")) {
            try {
                Claims claims=jwt.parse(header.substring(7)).getPayload();
                User user=users.findById(Long.valueOf(claims.getSubject())).orElse(null);
                if(user!=null && user.getStatus()==com.secureprint.entity.UserStatus.ACTIVE) {
                    var auth=new UsernamePasswordAuthenticationToken(user.getId(),null,java.util.List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (RuntimeException ignored) {
                request.setAttribute("invalidJwt", true);
            }
        }
        chain.doFilter(request,response);
    }
}
