package com.nelumbo.park.configuration.security.exception.exceptions;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException() {
        super();
    }

    public EmailNotFoundException(String message) {
        super(message);
    }
}
