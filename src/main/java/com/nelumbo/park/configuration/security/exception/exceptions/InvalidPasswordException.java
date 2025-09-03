package com.nelumbo.park.configuration.security.exception.exceptions;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super();
    }

    public InvalidPasswordException(String message) {
        super(message);
    }
}
