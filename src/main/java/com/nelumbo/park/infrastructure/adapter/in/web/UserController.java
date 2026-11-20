package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ManageUsersUseCase;
import com.nelumbo.park.application.port.in.ManageUsersUseCase.RegisterUserCommand;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.request.UserCreateRequest;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User API")
public class UserController {

    private final ManageUsersUseCase manageUsers;

    public UserController(ManageUsersUseCase manageUsers) {
        this.manageUsers = manageUsers;
    }

    @Operation(summary = "Obtiene todos los usuarios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios encontrados",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return manageUsers.listUsers().stream().map(WebMapper::toUserResponse).toList();
    }

    @Operation(summary = "Crea un nuevo usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "409", description = "El usuario o el email ya existe", content = @Content)
    })
    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserResponse createUser(@Validated @RequestBody UserCreateRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(request.getUsername(), request.getEmail(), request.getPassword());
        return WebMapper.toUserResponse(manageUsers.registerPartner(command));
    }
}
