package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.TokenResponse;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.configuration.security.JwtService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AuthMapper {

    @Autowired
    protected JwtService jwtService;

    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "accessToken", expression = "java(generateJwtToken(user))")
    @Mapping(target = "expiresAt", expression = "java(getExpirationTime())")
    public abstract TokenResponse toTokenResponse(User user);

    protected String generateJwtToken(User user) {
        System.out.println("=== GENERANDO TOKEN JWT ===");
        System.out.println("Usuario: " + user.getUsername());
        System.out.println("ID: " + user.getId());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Role: " + user.getRole());

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());

        String token = jwtService.generateToken(user.getUsername(), claims);
        System.out.println("Token generado: " + token);

        return token;
    }

    protected String getExpirationTime() {
        return LocalDateTime.now().plusHours(jwtService.getExpirationHours()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
