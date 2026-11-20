package com.nelumbo.park.infrastructure.exception;

public class DatabaseInitializationException extends RuntimeException {

    public DatabaseInitializationException(String message) {
        super(message);
    }

    public DatabaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
