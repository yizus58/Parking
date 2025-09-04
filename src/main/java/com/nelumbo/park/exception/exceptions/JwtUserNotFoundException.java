package com.nelumbo.park.exception.exceptions;

public class JwtUserNotFoundException extends RuntimeException {
    public JwtUserNotFoundException() {
        super();
    }

    public JwtUserNotFoundException(String message) {
        super(message);
    }
}
