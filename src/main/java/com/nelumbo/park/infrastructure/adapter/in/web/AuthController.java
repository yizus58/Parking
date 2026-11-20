package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.LoginUseCase;
import com.nelumbo.park.domain.exception.LoginBlockedException;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import org.springframework.http.HttpStatus;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.request.LoginRequest;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.UserLoginResponse;
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

    private final LoginUseCase loginUseCase;

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
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        try {
            LoginUseCase.AuthenticatedUser user = loginUseCase.login(
                    request.getEmail(), request.getPassword(), httpServletRequest.getRemoteAddr());
            return ResponseEntity.ok(WebMapper.toLoginResponse(user));
        } catch (LoginBlockedException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        }
    }
}
