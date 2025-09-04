package com.nelumbo.park.exception.exceptions;

public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException() {
        super();
    }

    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
