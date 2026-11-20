package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.ManageUsersUseCase.RegisterUserCommand;
import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.exception.DuplicateEmailException;
import com.nelumbo.park.domain.exception.DuplicateUsernameException;
import com.nelumbo.park.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserManagementServiceTest {

    private UserRepositoryPort users;
    private PasswordHasherPort hasher;
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        hasher = mock(PasswordHasherPort.class);
        service = new UserManagementService(users, hasher);
    }

    @Test
    void listUsers_returnsAll() {
        when(users.findAll()).thenReturn(List.of(Fixtures.admin()));
        assertEquals(1, service.listUsers().size());
    }

    @Test
    void registerPartner_hashesPasswordAndSetsRoleSocio() {
        when(users.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(users.findByUsername("newuser")).thenReturn(Optional.empty());
        when(hasher.hash("secret1")).thenReturn("hashed");
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.registerPartner(new RegisterUserCommand("newuser", "new@example.com", "secret1"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals("SOCIO", saved.getRole());
        assertNull(saved.getId());
    }

    @Test
    void registerPartner_duplicateEmail() {
        when(users.findByEmail("dup@example.com")).thenReturn(Optional.of(Fixtures.partner("p1")));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> service.registerPartner(new RegisterUserCommand("x", "dup@example.com", "secret1")));
        assertEquals("El email dup@example.com ya está registrado", ex.getMessage());
        verify(users, never()).save(any());
    }

    @Test
    void registerPartner_duplicateUsername() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(users.findByUsername("taken")).thenReturn(Optional.of(Fixtures.partner("p1")));

        assertThrows(DuplicateUsernameException.class,
                () -> service.registerPartner(new RegisterUserCommand("taken", "a@example.com", "secret1")));
        verify(users, never()).save(any());
    }
}
