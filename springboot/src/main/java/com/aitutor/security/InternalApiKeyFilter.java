package com.aitutor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces X-Internal-Api-Key at the filter layer for endpoints that FastAPI calls directly.
 * Complements the controller-level requireInternalKey() check (defense in depth).
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Set<String> INTERNAL_PATHS = Set.of(
            "/api/skills/active",
            "/api/skills/validation-set/active",
            "/api/skills/pipeline/result"
    );

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!INTERNAL_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader("X-Internal-Api-Key");
        if (!internalApiKey.equals(provided)) {
            log.warn("Rejected internal API request: missing or invalid key [uri={} ip={}]",
                    request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
