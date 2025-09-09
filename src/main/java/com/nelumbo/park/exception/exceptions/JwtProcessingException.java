package com.nelumbo.park.exception.exceptions;

public class JwtProcessingException extends RuntimeException {

    public JwtProcessingException(String message) {
        super(message);
    }

    public JwtProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}