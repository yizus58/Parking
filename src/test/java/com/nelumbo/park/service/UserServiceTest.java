package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.request.UserCreateRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.DuplicateEmailException;
import com.nelumbo.park.exception.exceptions.DuplicateUsernameException;
import com.nelumbo.park.exception.exceptions.EmailNotFoundException;
import com.nelumbo.park.exception.exceptions.InvalidPasswordException;
import com.nelumbo.park.mapper.AuthMapper;
import com.nelumbo.park.mapper.UserMapper;
import com.nelumbo.park.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserCreateRequest userCreateRequest;
    private UserResponse userResponse;
    private LoginRequest loginRequest;
    private UserLoginResponse userLoginResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-123");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("SOCIO");

        userCreateRequest = new UserCreateRequest();
        userCreateRequest.setUsername("testuser");
        userCreateRequest.setEmail("test@example.com");
        userCreateRequest.setPassword("password123");

        userResponse = new UserResponse();
        userResponse.setId("user-123");
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        userResponse.setRole("SOCIO");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        userLoginResponse = new UserLoginResponse();
        userLoginResponse.setId("user-123");
        userLoginResponse.setUsername("testuser");
        userLoginResponse.setEmail("test@example.com");
        userLoginResponse.setRole("SOCIO");
        userLoginResponse.setTokenType("Bearer");
        userLoginResponse.setAccessToken("jwt-token");
    }

    @Test
    void getAllUsers_ShouldReturnListOfUserResponses() {
        // Given
        User user2 = new User();
        user2.setId("user-456");
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setRole("ADMIN");

        UserResponse userResponse2 = new UserResponse();
        userResponse2.setId("user-456");
        userResponse2.setUsername("testuser2");
        userResponse2.setEmail("test2@example.com");
        userResponse2.setRole("ADMIN");

        List<User> users = Arrays.asList(user, user2);
        List<UserResponse> expectedResponses = Arrays.asList(userResponse, userResponse2);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);

        // When
        List<UserResponse> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedResponses.get(0).getId(), result.get(0).getId());
        assertEquals(expectedResponses.get(1).getId(), result.get(1).getId());

        verify(userRepository, times(1)).findAll();
        verify(userMapper, times(1)).toResponse(user);
        verify(userMapper, times(1)).toResponse(user2);
    }

    @Test
    void getAllUsers_WhenNoUsersExist_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<UserResponse> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository, times(1)).findAll();
        verify(userMapper, never()).toResponse(any(User.class));
    }

    @Test
    void createUser_WithValidData_ShouldReturnUserResponse() {
        // Given
        when(userRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(null);
        when(userRepository.findByName(userCreateRequest.getUsername())).thenReturn(null);
        when(userMapper.toEntity(userCreateRequest)).thenReturn(user);
        when(passwordEncoder.encode(userCreateRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        UserResponse result = userService.createUser(userCreateRequest);

        // Then
        assertNotNull(result);
        assertEquals(userResponse.getId(), result.getId());
        assertEquals(userResponse.getUsername(), result.getUsername());
        assertEquals(userResponse.getEmail(), result.getEmail());
        assertEquals(userResponse.getRole(), result.getRole());

        verify(userRepository, times(1)).findByEmail(userCreateRequest.getEmail());
        verify(userRepository, times(1)).findByName(userCreateRequest.getUsername());
        verify(userMapper, times(1)).toEntity(userCreateRequest);
        verify(passwordEncoder, times(1)).encode(userCreateRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toResponse(user);
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowDuplicateEmailException() {
        // Given
        when(userRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(user);

        // When & Then
        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> userService.createUser(userCreateRequest)
        );

        assertTrue(exception.getMessage().contains("test@example.com"));
        assertTrue(exception.getMessage().contains("ya está registrado"));

        verify(userRepository, times(1)).findByEmail(userCreateRequest.getEmail());
        verify(userRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldThrowDuplicateUsernameException() {
        // Given
        when(userRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(null);
        when(userRepository.findByName(userCreateRequest.getUsername())).thenReturn(user);

        // When & Then
        DuplicateUsernameException exception = assertThrows(
                DuplicateUsernameException.class,
                () -> userService.createUser(userCreateRequest)
        );

        assertTrue(exception.getMessage().contains("testuser"));
        assertTrue(exception.getMessage().contains("ya está registrado"));

        verify(userRepository, times(1)).findByEmail(userCreateRequest.getEmail());
        verify(userRepository, times(1)).findByName(userCreateRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserLoginResponse() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(authMapper.toUserLoginResponse(user)).thenReturn(userLoginResponse);

        // When
        UserLoginResponse result = userService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals(userLoginResponse.getId(), result.getId());
        assertEquals(userLoginResponse.getUsername(), result.getUsername());
        assertEquals(userLoginResponse.getEmail(), result.getEmail());
        assertEquals(userLoginResponse.getRole(), result.getRole());
        assertEquals(userLoginResponse.getTokenType(), result.getTokenType());
        assertEquals(userLoginResponse.getAccessToken(), result.getAccessToken());

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), user.getPassword());
        verify(authMapper, times(1)).toUserLoginResponse(user);
    }

    @Test
    void login_WithNonExistentEmail_ShouldThrowEmailNotFoundException() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(null);

        // When & Then
        assertThrows(EmailNotFoundException.class, () -> userService.login(loginRequest));

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(authMapper, never()).toUserLoginResponse(any(User.class));
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowInvalidPasswordException() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        // When & Then
        assertThrows(InvalidPasswordException.class, () -> userService.login(loginRequest));

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), user.getPassword());
        verify(authMapper, never()).toUserLoginResponse(any(User.class));
    }

    @Test
    void createUser_ShouldEncodePasswordCorrectly() {
        // Given
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword";

        userCreateRequest.setPassword(rawPassword);

        when(userRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(null);
        when(userRepository.findByName(userCreateRequest.getUsername())).thenReturn(null);
        when(userMapper.toEntity(userCreateRequest)).thenReturn(user);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        userService.createUser(userCreateRequest);

        // Then
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(argThat(savedUser ->
                savedUser.getPassword().equals(encodedPassword)
        ));
    }

    @Test
    void createUser_ShouldSetDefaultRole() {
        // Given
        when(userRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(null);
        when(userRepository.findByName(userCreateRequest.getUsername())).thenReturn(null);
        when(userMapper.toEntity(userCreateRequest)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        userService.createUser(userCreateRequest);

        // Then
        verify(userMapper, times(1)).toEntity(userCreateRequest);
        verify(userRepository, times(1)).save(any(User.class));
    }
}