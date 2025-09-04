package com.nelumbo.park.exception.exceptions;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException() {
        super();
    }

    public EmailNotFoundException(String message) {
        super(message);
    }
}
