package com.nelumbo.park.exception.exceptions;

public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException() {
        super();
    }
    
    public DuplicateEmailException(String message) {
        super(message);
    }
    
    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}