package com.example.mcp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String expectedApiKey;

    public ApiKeyFilter(@Value("${MCP_API_KEY:}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/mcp".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedApiKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "MCP_API_KEY is not configured");
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String suppliedApiKey = extractBearer(authorization);

        if (!constantTimeEquals(expectedApiKey, suppliedApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String extractBearer(String authorization) {
        if (authorization == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return authorization.substring(prefix.length()).trim();
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
