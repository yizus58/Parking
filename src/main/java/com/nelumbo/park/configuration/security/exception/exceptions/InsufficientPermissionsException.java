package com.nelumbo.park.configuration.security.exception.exceptions;

public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException() {
        super();
    }

    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
