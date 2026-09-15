package com.codegraph.bridge.security;

import com.codegraph.bridge.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    public AuthenticationFilter(
            SessionService sessionService) {

        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        /*
         * CORS preflight requests must not require authentication.
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            unauthorized(response);
            return;
        }

        String token =
                authorization.substring("Bearer ".length()).trim();

        if (!sessionService.isValid(token)) {

            unauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {

        return path.equals("/api/v1/auth/status")
                || path.equals("/api/v1/auth/pair")
                || path.equals("/api/v1/auth/reconnect")
                || path.equals("/api/v1/auth/verify")
                || path.equals("/error");
    }

    private void unauthorized(
            HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"success\":false,\"error\":\"Authentication required\"}"
        );
    }
}