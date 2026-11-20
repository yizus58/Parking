package com.nelumbo.park.domain.exception;

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