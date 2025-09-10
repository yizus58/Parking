package com.nelumbo.park.service.infrastructure;

import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.JwtUserNotFoundException;
import com.nelumbo.park.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }


        String id = authentication.getName();
        User user = userRepository.findByIdUser(id);

        if (user == null) {
            throw new JwtUserNotFoundException("El usuario no se encuentra en la base de datos. Por favor verifica tu correo electrónico.");
        }

        return user;
    }

    public boolean isAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }

    public boolean isSocio() {
        User currentUser = getCurrentUser();
        return currentUser != null && "SOCIO".equals(currentUser.getRole());
    }
}
