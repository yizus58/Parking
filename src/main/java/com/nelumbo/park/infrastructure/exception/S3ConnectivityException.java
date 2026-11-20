package com.nelumbo.park.infrastructure.exception;

public class S3ConnectivityException extends RuntimeException {

    public S3ConnectivityException(String message) {
        super(message);
    }

    public S3ConnectivityException(String message, Throwable cause) {
        super(message, cause);
    }
}
