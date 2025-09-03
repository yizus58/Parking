package com.nelumbo.park.configuration.security.exception.exceptions;

public class JwtUserNotFoundException extends RuntimeException {
    public JwtUserNotFoundException() {
        super();
    }

    public JwtUserNotFoundException(String message) {
        super(message);
    }
}
