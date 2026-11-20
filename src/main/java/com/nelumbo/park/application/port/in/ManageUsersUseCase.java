package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.User;

import java.util.List;

public interface ManageUsersUseCase {

    List<User> listUsers();

    /** Registers a partner (role SOCIO). */
    User registerPartner(RegisterUserCommand command);

    record RegisterUserCommand(String username, String email, String rawPassword) {
    }
}
