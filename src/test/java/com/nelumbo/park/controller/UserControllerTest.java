package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.request.UserCreateRequest;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    
    @MockBean
    private JwtService jwtService;
    
    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UserCreateRequest userCreateRequest;
    private UserResponse userResponse;
    private List<UserResponse> usersList;

    @BeforeEach
    void setUp() {
        userCreateRequest = new UserCreateRequest();
        userCreateRequest.setUsername("testuser");
        userCreateRequest.setEmail("test@example.com");
        userCreateRequest.setPassword("password123");

        userResponse = new UserResponse();
        userResponse.setId("user-123");
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        userResponse.setRole("SOCIO");

        UserResponse userResponse2 = new UserResponse();
        userResponse2.setId("user-456");
        userResponse2.setUsername("testuser2");
        userResponse2.setEmail("test2@example.com");
        userResponse2.setRole("ADMIN");

        usersList = Arrays.asList(userResponse, userResponse2);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getAllUsers_WithAdminRole_ShouldReturnUsersList() throws Exception {
        // Given
        when(userService.getAllUsers()).thenReturn(usersList);

        // When & Then
        mockMvc.perform(get("/users/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("user-123"))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].role").value("SOCIO"))
                .andExpect(jsonPath("$[1].id").value("user-456"))
                .andExpect(jsonPath("$[1].username").value("testuser2"))
                .andExpect(jsonPath("$[1].email").value("test2@example.com"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void getAllUsers_WithNonAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    void getAllUsers_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithValidData_ShouldReturnCreatedUser() throws Exception {
        // Given
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("SOCIO"));

        verify(userService, times(1)).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void createUser_WithNonAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    void createUser_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        userCreateRequest.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithBlankUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        userCreateRequest.setUsername("");

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithPasswordTooShort_ShouldReturnBadRequest() throws Exception {
        // Given
        userCreateRequest.setPassword("123");

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithInvalidUsernameCharacters_ShouldReturnBadRequest() throws Exception {
        // Given
        userCreateRequest.setUsername("invalid@username");

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithUsernameTooLong_ShouldReturnBadRequest() throws Exception {
        // Given
        userCreateRequest.setUsername("a".repeat(51));

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createUser_WithEmptyRequestBody_ShouldReturnBadRequest() throws Exception {
        // Given
        String emptyJson = "{}";

        // When & Then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateRequest.class));
    }
}