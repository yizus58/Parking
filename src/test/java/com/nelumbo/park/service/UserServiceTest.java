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
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);

        List<UserResponse> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void createUser_WithValidData_ShouldReturnUserResponse() {
        String rawPassword = userCreateRequest.getPassword();
        String encodedPassword = "encoded-password-for-test";

        User userToCreate = new User();
        userToCreate.setUsername(userCreateRequest.getUsername());
        userToCreate.setEmail(userCreateRequest.getEmail());
        userToCreate.setPassword(rawPassword);

        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(userRepository.findByName(anyString())).thenReturn(null);
        when(userMapper.toEntity(userCreateRequest)).thenReturn(userToCreate);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = userService.createUser(userCreateRequest);

        assertNotNull(result);
        assertEquals(userResponse.getId(), result.getId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(encodedPassword, savedUser.getPassword());
        verify(passwordEncoder, times(1)).encode(rawPassword);
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowDuplicateEmailException() {
        when(userRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(user);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(userCreateRequest));

        verify(userRepository, times(1)).findByEmail(userCreateRequest.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldThrowDuplicateUsernameException() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(userRepository.findByName(userCreateRequest.getUsername())).thenReturn(user);

        assertThrows(DuplicateUsernameException.class, () -> userService.createUser(userCreateRequest));

        verify(userRepository, times(1)).findByName(userCreateRequest.getUsername());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserLoginResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(authMapper.toUserLoginResponse(user)).thenReturn(userLoginResponse);

        UserLoginResponse result = userService.login(loginRequest, request);

        assertNotNull(result);
        assertEquals(userLoginResponse.getAccessToken(), result.getAccessToken());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), user.getPassword());
    }

    @Test
    void login_WithNonExistentEmail_ShouldThrowEmailNotFoundException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(null);

        assertThrows(EmailNotFoundException.class, () -> userService.login(loginRequest, request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowInvalidPasswordException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () -> userService.login(loginRequest, request));
        verify(authMapper, never()).toUserLoginResponse(any());
    }

    @Test
    void createUser_ShouldSetDefaultRole() {
        User userToCreate = new User();
        userToCreate.setPassword(userCreateRequest.getPassword());

        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(userRepository.findByName(anyString())).thenReturn(null);
        when(userMapper.toEntity(any(UserCreateRequest.class))).thenReturn(userToCreate);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        userService.createUser(userCreateRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("SOCIO", savedUser.getRole());
    }
}