package com.nelumbo.park.application.port.out;

import com.nelumbo.park.domain.model.User;

import java.util.Optional;

/**
 * The authenticated user of the current request.
 */
public interface CurrentUserPort {

    /**
     * @return empty when nobody is authenticated
     */
    Optional<User> currentUser();
}
