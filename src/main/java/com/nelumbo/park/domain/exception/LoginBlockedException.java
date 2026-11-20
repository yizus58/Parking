package com.nelumbo.park.domain.exception;

/**
 * Raised when a client IP is temporarily blocked after too many failed logins.
 */
public class LoginBlockedException extends RuntimeException {
    public LoginBlockedException(String message) {
        super(message);
    }
}
