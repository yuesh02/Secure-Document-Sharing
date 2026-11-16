package com.secureprint.config;
import com.secureprint.security.JwtAuthFilter;
import com.secureprint.security.RateLimitFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.Instant;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtAuthFilter jwt,RateLimitFilter rateLimit,
                                          @Value("${app.cors-allowed-origin}") String origin,
                                          @Value("${app.swagger-enabled:false}") boolean swaggerEnabled) throws Exception {
        http.csrf(csrf->csrf.disable()).cors(cors->cors.configurationSource(r->{var c=new org.springframework.web.cors.CorsConfiguration();c.setAllowedOrigins(java.util.List.of(origin));c.setAllowedMethods(java.util.List.of("GET","POST","PATCH","OPTIONS"));c.setAllowedHeaders(java.util.List.of("Authorization","Content-Type"));return c;}))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                    .httpStrictTransportSecurity(hsts->hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .exceptionHandling(e->e.authenticationEntryPoint(json401()).accessDeniedHandler(json403()))
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/auth/**","/error").permitAll();
                if (swaggerEnabled) a.requestMatchers("/swagger-ui/**","/v3/api-docs/**").permitAll();
                a.anyRequest().authenticated();
            })
            .addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    private AuthenticationEntryPoint json401(){return (req,res,e)->write(req,res,401,"UNAUTHENTICATED","Authentication required");}
    private AccessDeniedHandler json403(){return (req,res,e)->write(req,res,403,"FORBIDDEN","Access denied");}
    private void write(HttpServletRequest req,HttpServletResponse r,int status,String code,String message) throws IOException {r.setStatus(status);r.setContentType("application/json");new ObjectMapper().writeValue(r.getOutputStream(),Map.of("timestamp",Instant.now(),"status",status,"code",code,"message",message,"path",req.getRequestURI())); }
}
