package com.nelumbo.park.controller;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication API")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;

    @Operation(summary = "Login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserLoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas", content = @Content),
            @ApiResponse(responseCode = "403", description = "No se encontro el usuario", content = @Content),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return loginService.authenticate(request, httpServletRequest);
    }
}
