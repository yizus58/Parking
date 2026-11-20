package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Small helper over {@link CurrentUserPort} shared by the application services.
 */
@Component
class CurrentUserResolver {

    private final CurrentUserPort currentUserPort;

    CurrentUserResolver(CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
    }

    Optional<User> current() {
        return currentUserPort.currentUser();
    }

    User require() {
        return currentUserPort.currentUser().orElseThrow(InsufficientPermissionsException::new);
    }

    boolean isPartner(Optional<User> user) {
        return user.map(User::isPartner).orElse(false);
    }
}
