package com.nelumbo.park.infrastructure.config.security;

import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.model.User;
import io.jsonwebtoken.ExpiredJwtException;
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
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests that carry {@code Authorization: Bearer <jwt>}.
 *
 * <ul>
 *   <li>Expired token: 401 {"error":"Token expirado"}.</li>
 *   <li>Malformed token or wrong signature: 401 {"error":"Token JWT inválido o firma no válida"}.</li>
 *   <li>Valid token of a user that no longer exists: 403 {"error":"Usuario no encontrado"}.</li>
 * </ul>
 * The authority is the user's current role in the database, not the role
 * claim frozen in the token, so a changed role takes effect immediately.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String EXPIRED = "Token expirado";
    static final String INVALID = "Token JWT inválido o firma no válida";
    static final String USER_NOT_FOUND = "Usuario no encontrado";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepositoryPort userRepository;
    private final String applicationJson;
    private final String applicationJsonCharset;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepositoryPort userRepository,
            @Value("${application.json}") String applicationJson,
            @Value("${character.encoding}") String applicationJsonCharset
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.applicationJson = applicationJson;
        this.applicationJsonCharset = applicationJsonCharset;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId;
        try {
            userId = jwtService.extractUid(header.substring(BEARER.length()).trim());
        } catch (RuntimeException e) {
            boolean expired = causedBy(e, ExpiredJwtException.class);
            log.debug("Rejected JWT: {}", expired ? "expired" : "invalid");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, expired ? EXPIRED : INVALID);
            return;
        }
        if (userId == null || userId.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID);
            return;
        }

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, USER_NOT_FOUND);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(user.get().getRole())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private static boolean causedBy(Throwable error, Class<? extends Throwable> type) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(applicationJson);
        response.setCharacterEncoding(applicationJsonCharset);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
