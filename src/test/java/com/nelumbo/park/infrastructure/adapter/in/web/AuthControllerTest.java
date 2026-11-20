package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.LoginUseCase;
import com.nelumbo.park.application.port.in.LoginUseCase.AuthenticatedUser;
import com.nelumbo.park.domain.exception.EmailNotFoundException;
import com.nelumbo.park.domain.exception.InvalidPasswordException;
import com.nelumbo.park.domain.exception.LoginBlockedException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest extends WebLayerTestSupport {

    private static final String BODY = "{\"email\":\"socio@example.com\",\"password\":\"secret123\"}";

    @MockitoBean
    private LoginUseCase loginUseCase;

    @Test
    void login_returnsUserAndBearerToken() throws Exception {
        when(loginUseCase.login(eq("socio@example.com"), eq("secret123"), anyString()))
                .thenReturn(new AuthenticatedUser(WebFixtures.owner(), "jwt-token"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("owner-1"))
                .andExpect(jsonPath("$.username").value("socio"))
                .andExpect(jsonPath("$.email").value("socio@example.com"))
                .andExpect(jsonPath("$.role").value("SOCIO"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void login_blockedIp_returns429WithMessage() throws Exception {
        when(loginUseCase.login(any(), any(), any()))
                .thenThrow(new LoginBlockedException("Lo sentimos no puedes hacer login, durante 5 min"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Lo sentimos no puedes hacer login, durante 5 min"));
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        when(loginUseCase.login(any(), any(), any())).thenThrow(new EmailNotFoundException());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email no encontrado"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        when(loginUseCase.login(any(), any(), any())).thenThrow(new InvalidPasswordException());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Password incorrecta"));
    }

    @Test
    void login_withoutCsrfToken_isAccepted() throws Exception {
        // CSRF is disabled for this stateless JWT API: a plain JSON POST, as any client sends it, reaches the use case.
        when(loginUseCase.login(any(), any(), any())).thenThrow(new InvalidPasswordException());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(loginUseCase).login(any(), any(), any());
    }

    @Test
    void login_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
        verify(loginUseCase, never()).login(any(), any(), any());
    }
}
