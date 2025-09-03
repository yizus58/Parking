package com.nelumbo.park.configuration.security.exception.exceptions;

public class DuplicateUsernameException extends RuntimeException {
    
    public DuplicateUsernameException() {
        super();
    }
    
    public DuplicateUsernameException(String message) {
        super(message);
    }
    
    public DuplicateUsernameException(String message, Throwable cause) {
        super(message, cause);
    }
}