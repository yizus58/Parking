package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.User;

public interface LoginUseCase {

    /**
     * @throws com.nelumbo.park.domain.exception.LoginBlockedException when the IP is blocked
     * @throws com.nelumbo.park.domain.exception.EmailNotFoundException  unknown email
     * @throws com.nelumbo.park.domain.exception.InvalidPasswordException wrong password
     */
    AuthenticatedUser login(String email, String password, String clientIp);

    record AuthenticatedUser(User user, String accessToken) {
    }
}
