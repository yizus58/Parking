package com.nelumbo.park.exception.exceptions;

public class BackoffExecutionFailedException extends RuntimeException {
    public BackoffExecutionFailedException() {
        super();
    }

    public BackoffExecutionFailedException(String message) {
        super(message);
    }

    public BackoffExecutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}