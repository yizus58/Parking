package com.nelumbo.park.application.port.out;

import com.nelumbo.park.domain.model.User;

public interface AccessTokenPort {

    String issueToken(User user);
}
