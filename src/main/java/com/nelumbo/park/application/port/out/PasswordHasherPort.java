package com.nelumbo.park.application.port.out;

public interface PasswordHasherPort {

    String hash(String rawPassword);

    /**
     * When {@code hash} is null the implementation still does an equivalent check against a dummy hash
     * and returns false, so callers do not leak through timing whether a user exists.
     */
    boolean matches(String rawPassword, String hash);
}
