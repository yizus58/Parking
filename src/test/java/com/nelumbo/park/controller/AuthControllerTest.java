package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.interfaces.ILoginLogService;
import com.nelumbo.park.interfaces.LoginAttemptService;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.UserService;
import com.nelumbo.park.service.implementations.LoginServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, LoginServiceImpl.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LoginAttemptService loginAttemptService;

    @MockBean
    private ILoginLogService loginLogService;

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
        when(userService.login(any(LoginRequest.class))).thenReturn(userLoginResponse);
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);

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

        verify(userService, times(1)).login(any(LoginRequest.class));
        verify(loginAttemptService, times(1)).loginSucceeded(anyString());
        verify(loginLogService, times(1)).save(anyString(), anyString(), anyString(), any());
    }

    @Test
    void login_WithInvalidRequestBody_ShouldReturnBadRequest() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_WithEmptyRequestBody_ShouldReturnBadRequest() throws Exception {
        String emptyJson = "{}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_WithMissingContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        loginRequest.setEmail("invalid-email");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_WithBlankPassword_ShouldReturnBadRequest() throws Exception {
        loginRequest.setPassword("");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_WithPasswordTooShort_ShouldReturnBadRequest() throws Exception {
        loginRequest.setPassword("admin");
        loginRequest.setEmail("admin@mail.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_WhenIpIsBlocked_ShouldReturnTooManyRequests() throws Exception {
        when(loginAttemptService.isBlocked(anyString())).thenReturn(true);
        when(loginAttemptService.getBlockedUntil(anyString())).thenReturn(LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lo sentimos no puedes hacer login")));

        verify(userService, never()).login(any(LoginRequest.class));
        verify(loginAttemptService, times(1)).isBlocked(anyString());
        verify(loginAttemptService, times(1)).getBlockedUntil(anyString());
        verify(loginAttemptService, never()).loginSucceeded(anyString());
        verify(loginAttemptService, never()).loginFailed(anyString(), anyString());
        verify(loginLogService, never()).save(anyString(), anyString(), anyString(), any());
    }
}
