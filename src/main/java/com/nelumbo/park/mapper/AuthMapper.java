package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.response.TokenResponse;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.config.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@RequiredArgsConstructor
public abstract class AuthMapper {

    private final JwtService jwtService;

    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "accessToken", expression = "java(generateJwtToken(user))")
    public abstract TokenResponse toTokenResponse(User user);

    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "accessToken", expression = "java(generateJwtToken(user))")
    @Mapping(target = "role", source = "user.role")
    public abstract UserLoginResponse toUserLoginResponse(User user);


    protected String generateJwtToken(User user) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());

        return jwtService.generateToken(user.getUsername(), claims);
    }

    protected String getExpirationTime() {
        return LocalDateTime.now().plusHours(jwtService.expirationHours()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
