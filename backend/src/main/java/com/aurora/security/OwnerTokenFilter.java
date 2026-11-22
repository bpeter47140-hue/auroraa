package com.aurora.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Profile("legacy-owner-token")
public class OwnerTokenFilter extends OncePerRequestFilter {
    private final byte[] expectedToken;

    public OwnerTokenFilter(@Value("${AURORA_OWNER_TOKEN:}") String token) {
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("Cloud profile requires AURORA_OWNER_TOKEN with at least 32 characters.");
        }
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
            || "/api/v1/system/status".equals(path)
            || "/actuator/health".equals(path)
            || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Baseline browser protections for the current private prototype.
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String authorization = request.getHeader("Authorization");
        String supplied = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7) : "";
        boolean valid = MessageDigest.isEqual(
            expectedToken, supplied.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Owner authentication required\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
