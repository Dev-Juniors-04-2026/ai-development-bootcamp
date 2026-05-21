package com.example.todoapp.adapter.in.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        UUID userId;
        try {
            userId = jwtService.verify(token);
        } catch (JwtException | IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }
}
