package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.TokenResponse;
import com.nelumbo.park.dto.request.UserCreateRequest;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.configuration.security.exception.exceptions.EmailNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.InvalidPasswordException;
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void createUser(UserCreateRequest userCreateRequest) {
        User user = userMapper.toEntity(userCreateRequest);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            logger.warn("Usuario no encontrado con email: {}", loginRequest.getEmail());
            throw new EmailNotFoundException();
        }

        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!passwordMatches) {
            logger.warn("Contraseña incorrecta para el usuario: {}", loginRequest.getEmail());
            throw new InvalidPasswordException();
        }

        return authMapper.toTokenResponse(user);
    }
}
