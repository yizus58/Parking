package com.nelumbo.park.configuration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtService.extractUsername(jwt);
                logger.info("Token JWT encontrado para usuario: {}", username);
            } catch (Exception e) {
                logger.error("Error al extraer username del token: {}", e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (!jwtService.isTokenExpired(jwt)) {
                    String role = jwtService.extractRole(jwt);
                    String email = jwtService.extractEmail(jwt);
                    String userId = jwtService.extractUserId(jwt);

                    logger.info("Token válido - Usuario: {}, Role: {}, Email: {}", username, role, email);

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                    );

                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.info("Autenticación establecida para usuario: {} con authorities: {}", 
                        username, authorities);
                } else {
                    logger.warn("Token expirado para usuario: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error al validar token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
