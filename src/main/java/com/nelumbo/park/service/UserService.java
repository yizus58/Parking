package com.nelumbo.park.service;

import com.nelumbo.park.dto.LoginRequest;
import com.nelumbo.park.dto.TokenResponse;
import com.nelumbo.park.dto.UserCreateRequest;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.mapper.AuthMapper;
import com.nelumbo.park.mapper.UserMapper;
import com.nelumbo.park.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        try {
            User user = userRepository.findByEmail(loginRequest.getEmail());

            if (user == null) {
                logger.warn("Usuario no encontrado con email: {}", loginRequest.getEmail());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
            }

            boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

            if (!passwordMatches) {
                logger.warn("Contraseña incorrecta para el usuario: {}", loginRequest.getEmail());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
            }

            return authMapper.toTokenResponse(user);

        } catch (ResponseStatusException e) {
            logger.error("Error de autenticación: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en login", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        }
    }
}
