package com.nelumbo.park.configuration.initialization;

import com.nelumbo.park.entity.User;
import com.nelumbo.park.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initialize() {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        if (userRepository.findByEmail("admin@mail.com") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEmail("admin@mail.com");
            admin.setRole("ADMIN");

            userRepository.save(admin);
        }
    }
}
