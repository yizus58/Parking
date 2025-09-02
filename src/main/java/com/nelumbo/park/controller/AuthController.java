package com.nelumbo.park.controller;

import com.nelumbo.park.dto.LoginRequest;
import com.nelumbo.park.dto.TokenResponse;
import com.nelumbo.park.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse tokenResponse = userService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }
}
