package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.dto.request.UserCreateRequest;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.EmailNotFoundException;
import com.nelumbo.park.exception.exceptions.DuplicateEmailException;
import com.nelumbo.park.exception.exceptions.DuplicateUsernameException;
import com.nelumbo.park.exception.exceptions.InvalidPasswordException;
import com.nelumbo.park.mapper.AuthMapper;
import com.nelumbo.park.mapper.UserMapper;
import com.nelumbo.park.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            AuthMapper authMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse createUser(UserCreateRequest userCreateRequest) {
        User existingUserByEmail = userRepository.findByEmail(userCreateRequest.getEmail());
        if (existingUserByEmail != null) {
            throw new DuplicateEmailException("El email " + userCreateRequest.getEmail() + " ya está registrado");
        }

        User existingUserByUsername = userRepository.findByName(userCreateRequest.getUsername());
        if (existingUserByUsername != null) {
            throw new DuplicateUsernameException("El nombre de usuario " + userCreateRequest.getUsername() + " ya está registrado");
        }

        User user = userMapper.toEntity(userCreateRequest);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserLoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user == null) {
            throw new EmailNotFoundException();
        }

        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!passwordMatches) {
            logger.warn("Contraseña incorrecta para el usuario: {}", loginRequest.getEmail());
            throw new InvalidPasswordException();
        }
        return authMapper.toUserLoginResponse(user);
    }
}
