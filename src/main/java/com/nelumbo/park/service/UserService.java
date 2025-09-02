package com.nelumbo.park.service;

import com.nelumbo.park.dto.LoginRequest;
import com.nelumbo.park.dto.TokenResponse;
import com.nelumbo.park.dto.UserCreateRequest;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.mapper.AuthMapper;
import com.nelumbo.park.mapper.UserMapper;
import com.nelumbo.park.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            AuthMapper authMapper
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void createUser(UserCreateRequest userCreateRequest) {
        User user = userMapper.toEntity(userCreateRequest);
        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        if (!loginRequest.getPassword().equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        return authMapper.toTokenResponse(user);
    }
}
