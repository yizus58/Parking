package com.nelumbo.park.infrastructure.adapter.out.security;

import com.nelumbo.park.application.port.out.AccessTokenPort;
import com.nelumbo.park.infrastructure.config.security.JwtService;
import com.nelumbo.park.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Issues the same JWT as before: subject = username, claims uid, email, role.
 */
@Component
public class JwtAccessTokenAdapter implements AccessTokenPort {

    private final JwtService jwtService;

    public JwtAccessTokenAdapter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        return jwtService.generateToken(user.getUsername(), claims);
    }
}
