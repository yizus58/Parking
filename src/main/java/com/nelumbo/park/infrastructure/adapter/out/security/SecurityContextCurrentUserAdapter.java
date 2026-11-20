package com.nelumbo.park.infrastructure.adapter.out.security;

import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.infrastructure.exception.JwtUserNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the authenticated user. The JWT filter stores the user id as the
 * authentication name.
 */
@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    private final UserRepositoryPort userRepository;

    public SecurityContextCurrentUserAdapter(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        User user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new JwtUserNotFoundException(
                        "El usuario no se encuentra en la base de datos. Por favor verifica tu correo electrónico."));
        return Optional.of(user);
    }
}
