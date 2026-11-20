package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.User;

public interface LoginUseCase {

    /**
     * @throws com.nelumbo.park.domain.exception.LoginBlockedException when the IP is blocked
     * @throws com.nelumbo.park.domain.exception.InvalidCredentialsException unknown email or wrong password (same error)
     */
    AuthenticatedUser login(String email, String password, String clientIp);

    record AuthenticatedUser(User user, String accessToken) {
    }
}
