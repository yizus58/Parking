package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ManageUsersUseCase;
import com.nelumbo.park.application.port.in.ManageUsersUseCase.RegisterUserCommand;
import com.nelumbo.park.domain.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends WebLayerTestSupport {

    private static final String VALID = "{\"username\":\"nuevo.socio\",\"email\":\"nuevo@example.com\",\"password\":\"secret123\"}";

    @MockitoBean
    private ManageUsersUseCase manageUsers;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void list_asAdmin() throws Exception {
        when(manageUsers.listUsers()).thenReturn(List.of(WebFixtures.owner()));

        mockMvc.perform(get("/users/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("owner-1"))
                .andExpect(jsonPath("$[0].username").value("socio"))
                .andExpect(jsonPath("$[0].role").value("SOCIO"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void list_asPartner_forbidden() throws Exception {
        mockMvc.perform(get("/users/"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permisos para realizar esta acción"));
        verify(manageUsers, never()).listUsers();
    }

    @Test
    void list_anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/users/")).andExpect(status().isUnauthorized());
        verify(manageUsers, never()).listUsers();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_returnsUser() throws Exception {
        when(manageUsers.registerPartner(new RegisterUserCommand("nuevo.socio", "nuevo@example.com", "secret123")))
                .thenReturn(new com.nelumbo.park.domain.model.User("u9", "nuevo.socio", "hash", "nuevo@example.com", "SOCIO"));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u9"))
                .andExpect(jsonPath("$.role").value("SOCIO"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_invalid_returns400() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name!\",\"email\":\"x\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest());
        verify(manageUsers, never()).registerPartner(any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_duplicateEmail_returns409() throws Exception {
        when(manageUsers.registerPartner(any())).thenThrow(new DuplicateEmailException("dup"));

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El email ya está registrado"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void create_asPartner_forbidden() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
        verify(manageUsers, never()).registerPartner(any());
    }
}
