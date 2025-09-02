package com.nelumbo.park.controller;

import com.nelumbo.park.dto.LoginRequest;
import com.nelumbo.park.dto.TokenResponse;
import com.nelumbo.park.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {

        try {
            TokenResponse tokenResponse = userService.login(request);
            return ResponseEntity.ok(tokenResponse);
        } catch (ResponseStatusException e) {
            logger.error("Error en login para email: {} - Status: {} - Mensaje: {}", 
                request.getEmail(), e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en login para email: {}", request.getEmail(), e);
            throw e;
        }
    }
}
