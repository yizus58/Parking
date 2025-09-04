package com.nelumbo.park.controller;

import com.nelumbo.park.dto.request.UserCreateRequest;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.service.UserService;
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
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @GetMapping("/")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserResponse createUser(@Validated @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }
}
