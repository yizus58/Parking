package com.nelumbo.park.domain.model;

import java.time.Instant;
import java.util.UUID;

public record LoginLog(UUID id, String username, String email, String description, Instant date) {

    public static LoginLog of(String username, String email, String description, Instant date) {
        return new LoginLog(null, username, email, description, date);
    }
}
