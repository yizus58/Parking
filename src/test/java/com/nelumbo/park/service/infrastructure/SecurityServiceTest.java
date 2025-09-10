package com.nelumbo.park.service.infrastructure;

import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.JwtUserNotFoundException;
import com.nelumbo.park.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    @Spy
    private SecurityService securityService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    // --- getCurrentUser tests ---

    @Test
    @DisplayName("Should return null when authentication is null")
    void getCurrentUser_AuthenticationIsNull_ReturnsNull() {
        SecurityContext securityContext = mock(SecurityContext.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        User result = securityService.getCurrentUser();

        assertNull(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return null when user is not authenticated")
    void getCurrentUser_NotAuthenticated_ReturnsNull() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        User result = securityService.getCurrentUser();

        assertNull(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return current user when authenticated and user found")
    void getCurrentUser_AuthenticatedAndUserFound_ReturnsUser() {
        String userId = "testUser";
        User expectedUser = new User();
        expectedUser.setId(userId);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userId);
        when(userRepository.findByIdUser(userId)).thenReturn(expectedUser);

        User result = securityService.getCurrentUser();

        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository, times(1)).findByIdUser(userId);
    }

    @Test
    @DisplayName("Should throw JwtUserNotFoundException when authenticated but user not found in DB")
    void getCurrentUser_AuthenticatedButUserNotFound_ThrowsException() {
        String userId = "nonExistentUser";

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userId);
        when(userRepository.findByIdUser(userId)).thenReturn(null);

        JwtUserNotFoundException thrown = assertThrows(JwtUserNotFoundException.class, () -> {
            securityService.getCurrentUser();
        });

        assertTrue(thrown.getMessage().contains("El usuario no se encuentra en la base de datos"));
        verify(userRepository, times(1)).findByIdUser(userId);
    }

    // --- isAdmin tests ---

    @Test
    @DisplayName("Should return true if current user is ADMIN")
    void isAdmin_UserIsAdmin_ReturnsTrue() {

        User adminUser = new User();
        adminUser.setRole("ADMIN");

        doReturn(adminUser).when(securityService).getCurrentUser();


        boolean result = securityService.isAdmin();


        assertTrue(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if current user is not ADMIN")
    void isAdmin_UserIsNotAdmin_ReturnsFalse() {

        User socioUser = new User();
        socioUser.setRole("SOCIO");

        doReturn(socioUser).when(securityService).getCurrentUser();


        boolean result = securityService.isAdmin();


        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if no current user for isAdmin check")
    void isAdmin_NoCurrentUser_ReturnsFalse() {

        doReturn(null).when(securityService).getCurrentUser();

        boolean result = securityService.isAdmin();

        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    // --- isSocio tests ---

    @Test
    @DisplayName("Should return true if current user is SOCIO")
    void isSocio_UserIsSocio_ReturnsTrue() {

        User socioUser = new User();
        socioUser.setRole("SOCIO");

        doReturn(socioUser).when(securityService).getCurrentUser();

        boolean result = securityService.isSocio();

        assertTrue(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if current user is not SOCIO")
    void isSocio_UserIsNotSocio_ReturnsFalse() {

        User adminUser = new User();
        adminUser.setRole("ADMIN");

        doReturn(adminUser).when(securityService).getCurrentUser();

        boolean result = securityService.isSocio();

        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if no current user for isSocio check")
    void isSocio_NoCurrentUser_ReturnsFalse() {

        doReturn(null).when(securityService).getCurrentUser();

        boolean result = securityService.isSocio();

        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }
}
