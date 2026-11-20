package com.nelumbo.park.infrastructure.config.initialization;

import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.model.Role;
import com.nelumbo.park.domain.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    @Value("${admin.password}")
    private String passwordAdmin;

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public DatabaseInitializer(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @PostConstruct
    public void initialize() {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        if (userRepository.findByEmail("admin@mail.com").isEmpty()) {
            User admin = new User(null, "admin", passwordHasher.hash(passwordAdmin), "admin@mail.com", Role.ADMIN.name());
            userRepository.save(admin);
        }
    }
}
