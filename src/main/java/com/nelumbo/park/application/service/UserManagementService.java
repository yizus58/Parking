package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.ManageUsersUseCase;
import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.exception.DuplicateEmailException;
import com.nelumbo.park.domain.exception.DuplicateUsernameException;
import com.nelumbo.park.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementService implements ManageUsersUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public UserManagementService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User registerPartner(RegisterUserCommand command) {
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new DuplicateEmailException("El email " + command.email() + " ya está registrado");
        }
        if (userRepository.findByUsername(command.username()).isPresent()) {
            throw new DuplicateUsernameException("El nombre de usuario " + command.username() + " ya está registrado");
        }
        User user = User.newPartner(command.username(), command.email(), passwordHasher.hash(command.rawPassword()));
        return userRepository.save(user);
    }
}
