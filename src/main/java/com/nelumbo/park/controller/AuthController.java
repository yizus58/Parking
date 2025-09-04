package com.nelumbo.park.controller;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.TokenResponse;
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
        } catch (Exception e) {
            throw e;
        }
    }
}
