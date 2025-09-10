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
    private SecurityService securityService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        // Mock the static SecurityContextHolder
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    // --- getCurrentUser tests ---

    @Test
    @DisplayName("Should return null when authentication is null")
    void getCurrentUser_AuthenticationIsNull_ReturnsNull() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        User result = securityService.getCurrentUser();

        // Then
        assertNull(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return null when user is not authenticated")
    void getCurrentUser_NotAuthenticated_ReturnsNull() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        User result = securityService.getCurrentUser();

        // Then
        assertNull(result);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return current user when authenticated and user found")
    void getCurrentUser_AuthenticatedAndUserFound_ReturnsUser() {
        // Given
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

        // When
        User result = securityService.getCurrentUser();

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository, times(1)).findByIdUser(userId);
    }

    @Test
    @DisplayName("Should throw JwtUserNotFoundException when authenticated but user not found in DB")
    void getCurrentUser_AuthenticatedButUserNotFound_ThrowsException() {
        // Given
        String userId = "nonExistentUser";

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userId);
        when(userRepository.findByIdUser(userId)).thenReturn(null);

        // When / Then
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
        // Given
        User adminUser = new User();
        adminUser.setRole("ADMIN");
        // Mock getCurrentUser to return an ADMIN user
        doReturn(adminUser).when(securityService).getCurrentUser();

        // When
        boolean result = securityService.isAdmin();

        // Then
        assertTrue(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if current user is not ADMIN")
    void isAdmin_UserIsNotAdmin_ReturnsFalse() {
        // Given
        User socioUser = new User();
        socioUser.setRole("SOCIO");
        // Mock getCurrentUser to return a SOCIO user
        doReturn(socioUser).when(securityService).getCurrentUser();

        // When
        boolean result = securityService.isAdmin();

        // Then
        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if no current user for isAdmin check")
    void isAdmin_NoCurrentUser_ReturnsFalse() {
        // Given
        // Mock getCurrentUser to return null
        doReturn(null).when(securityService).getCurrentUser();

        // When
        boolean result = securityService.isAdmin();

        // Then
        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    // --- isSocio tests ---

    @Test
    @DisplayName("Should return true if current user is SOCIO")
    void isSocio_UserIsSocio_ReturnsTrue() {
        // Given
        User socioUser = new User();
        socioUser.setRole("SOCIO");
        // Mock getCurrentUser to return a SOCIO user
        doReturn(socioUser).when(securityService).getCurrentUser();

        // When
        boolean result = securityService.isSocio();

        // Then
        assertTrue(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if current user is not SOCIO")
    void isSocio_UserIsNotSocio_ReturnsFalse() {
        // Given
        User adminUser = new User();
        adminUser.setRole("ADMIN");
        // Mock getCurrentUser to return an ADMIN user
        doReturn(adminUser).when(securityService).getCurrentUser();

        // When
        boolean result = securityService.isSocio();

        // Then
        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("Should return false if no current user for isSocio check")
    void isSocio_NoCurrentUser_ReturnsFalse() {
        // Given
        // Mock getCurrentUser to return null
        doReturn(null).when(securityService).getCurrentUser();

        // When
        boolean result = securityService.isSocio();

        // Then
        assertFalse(result);
        verify(securityService, times(1)).getCurrentUser();
    }

    // Close the mocked static context after all tests in the class
    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }
}
