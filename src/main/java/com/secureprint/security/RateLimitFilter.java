package com.secureprint.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int limit;
    private final long windowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.requests:20}") int limit,
                           @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.limit = limit;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isLimited(request)) {
            chain.doFilter(request, response);
            return;
        }
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= windowMillis
                        ? new Window(now) : current);
        if (window.count.incrementAndGet() > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", Long.toString(Math.max(1, (windowMillis - (now - window.startedAt)) / 1000)));
            new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                    "timestamp", Instant.now(), "status", 429, "code", "RATE_LIMITED",
                    "message", "Too many requests", "path", request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isLimited(HttpServletRequest request) {
        if ("POST".equals(request.getMethod()) && "/auth/login".equals(request.getRequestURI())) return true;
        if (!"POST".equals(request.getMethod())) return false;
        String path = request.getRequestURI();
        return "/requests".equals(path) || "/requests/edit".equals(path)
                || "/access/view".equals(path) || "/access/download".equals(path);
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger();
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
