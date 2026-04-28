package com.example.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AdminTokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String ADMIN_PATH_PREFIX = "/api/admin";
    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String adminSyncToken;

    public AdminTokenAuthenticationFilter(@Value("${admin.sync.token:}") String adminSyncToken) {
        this.adminSyncToken = adminSyncToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isAdminPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isValidToken(extractToken(request))) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid admin token");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String headerToken = request.getHeader(ADMIN_TOKEN_HEADER);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }

        return "";
    }

    private boolean isValidToken(String token) {
        return adminSyncToken != null && !adminSyncToken.isBlank() && adminSyncToken.equals(token);
    }

    private boolean isAdminPath(String path) {
        return ADMIN_PATH_PREFIX.equals(path) || path.startsWith(ADMIN_PATH_PREFIX + "/");
    }
}
