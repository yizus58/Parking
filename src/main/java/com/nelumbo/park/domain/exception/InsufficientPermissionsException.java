package com.nelumbo.park.domain.exception;

public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException() {
        super();
    }

    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
