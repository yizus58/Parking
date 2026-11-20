package com.nelumbo.park.infrastructure.exception;

public class JwtUserNotFoundException extends RuntimeException {
    public JwtUserNotFoundException() {
        super();
    }

    public JwtUserNotFoundException(String message) {
        super(message);
    }
}
