package com.nelumbo.park.exception.exceptions;

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