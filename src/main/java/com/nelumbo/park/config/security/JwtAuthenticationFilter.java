package com.nelumbo.park.config.security;

import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.JwtProcessingException;
import com.nelumbo.park.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger loggers = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String applicationJson;
    private final String applicationJsonCharset;


    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${application.json}") String applicationJson,
            @Value("${character.encoding}") String applicationJsonCharset
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.applicationJson = applicationJson;
        this.applicationJsonCharset = applicationJsonCharset;
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
                username = extractUsernameFromJwt(jwt, response);
                if (username == null) {
                    return;
                }
            } catch (JwtProcessingException e) {
                loggers.error("Error processing JWT: {}", e.getMessage());
                writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al procesar token JWT");
                return;
            }
        }

        if (username != null && !validateUserExists(username, response)) {
            return;
        }

        if (username != null) {
            boolean isNotAuthenticated = SecurityContextHolder.getContext().getAuthentication() == null;
            if (isNotAuthenticated) {
                boolean authenticationFailed = !processAuthentication(jwt, username, response);
                if (authenticationFailed) {
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String errorMessage) throws IOException {
        response.setStatus(status);
        response.setContentType(applicationJson);
        response.setCharacterEncoding(applicationJsonCharset);
        response.getWriter().write("{\"error\":\"" + errorMessage + "\"}");
    }

    private String extractUsernameFromJwt(String jwt, HttpServletResponse response) throws IOException {
        try {
            return jwtService.extractUid(jwt);
        } catch (RuntimeException e) {
            if (isSignatureError(e)) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Token JWT inválido o firma no válida");
                return null;
            }
            throw new JwtProcessingException("Error al procesar token JWT para extracción de username", e);
        }
    }

    private boolean isSignatureError(Exception e) {
        String message = e.getMessage();
        return message.contains("JWT signature does not match") || 
               message.contains("signature") || 
               message.contains("SignatureException");
    }

    private boolean validateUserExists(String username, HttpServletResponse response) throws IOException {
        Optional<User> userOptional = userRepository.findById(username);
        if (userOptional.isEmpty()) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Usuario no encontrado");
            return false;
        }
        return true;
    }


    private boolean processAuthentication(String jwt, String username, HttpServletResponse response) throws IOException {
        try {
            boolean tokenNotExpired = !jwtService.isTokenExpired(jwt);
            if (tokenNotExpired) {
                setAuthentication(jwt, username);
                return true;
            } else {
                loggers.warn("Token expirado para usuario: {}", username);
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
                return false;
            }
        } catch (Exception e) {
            loggers.error("Error al validar token: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al validar token");
            return false;
        }
    }

    private void setAuthentication(String jwt, String username) {
        String role = jwtService.extractRole(jwt);
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}