package com.nelumbo.park.infrastructure.exception;

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