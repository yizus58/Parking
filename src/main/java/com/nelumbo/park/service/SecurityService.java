package com.nelumbo.park.service;

import com.nelumbo.park.entity.User;
import com.nelumbo.park.configuration.security.exception.exceptions.JwtUserNotFoundException;
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


        String email = authentication.getName();
        System.out.println("email extraído: " + email);
        User user = userRepository.findByEmail(email);
        System.out.println("user extraído: " + user);

        if (user == null) {
            throw new JwtUserNotFoundException("El usuario no se encuentra en la base de datos. Por favor verifica tu correo electrónico.");
        }

        return user;
    }

    public boolean isAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }

    public boolean isEmpleado() {
        User currentUser = getCurrentUser();
        return currentUser != null && "EMPLEADO".equals(currentUser.getRole());
    }
}
