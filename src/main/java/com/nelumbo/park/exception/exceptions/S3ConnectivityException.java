package com.nelumbo.park.exception.exceptions;

public class S3ConnectivityException extends RuntimeException {

    public S3ConnectivityException(String message) {
        super(message);
    }

    public S3ConnectivityException(String message, Throwable cause) {
        super(message, cause);
    }
}
