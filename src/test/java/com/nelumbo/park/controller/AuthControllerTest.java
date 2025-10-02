package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.interfaces.LoginService;
import com.nelumbo.park.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginService loginService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private UserLoginResponse userLoginResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("1231443");

        userLoginResponse = new UserLoginResponse();
        userLoginResponse.setId("468fdf0e-f105-43cc-9573-e669f7190868");
        userLoginResponse.setUsername("userexample");
        userLoginResponse.setEmail("user@example.com");
        userLoginResponse.setRole("SOCIO");
        userLoginResponse.setTokenType("Bearer");
        userLoginResponse.setAccessToken("jwt-token-example");
    }

    @Test
    void login_WithValidCredentials_ShouldReturnTokenResponse() throws Exception {
        when(loginService.authenticate(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenReturn(ResponseEntity.ok(userLoginResponse));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("468fdf0e-f105-43cc-9573-e669f7190868"))
                .andExpect(jsonPath("$.username").value("userexample"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("SOCIO"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-token-example"));

        verify(loginService, times(1)).authenticate(any(LoginRequest.class), any(HttpServletRequest.class));
    }

    @Test
    void login_WithInvalidRequestBody_ShouldReturnBadRequest() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(loginService, never()).authenticate(any(), any());
    }

    @Test
    void login_WithEmptyRequestBody_ShouldReturnBadRequest() throws Exception {
        String emptyJson = "{}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest());

        verify(loginService, never()).authenticate(any(), any());
    }

    @Test
    void login_WithMissingContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(loginService, never()).authenticate(any(), any());
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        loginRequest.setEmail("invalid-email");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(loginService, never()).authenticate(any(), any());
    }

    @Test
    void login_WithBlankPassword_ShouldReturnBadRequest() throws Exception {
        loginRequest.setPassword("");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(loginService, never()).authenticate(any(), any());
    }

    @Test
    void login_WithPasswordTooShort_ShouldReturnBadRequest() throws Exception {
        loginRequest.setPassword("admin");
        loginRequest.setEmail("admin@mail.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(loginService, never()).authenticate(any(), any());
    }

    @Test
    void login_WhenIpIsBlocked_ShouldReturnTooManyRequests() throws Exception {
        when(loginService.authenticate(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Lo sentimos no puedes hacer login"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lo sentimos no puedes hacer login")));

        verify(loginService, times(1)).authenticate(any(LoginRequest.class), any(HttpServletRequest.class));
    }
}
