package com.nelumbo.park.infrastructure.exception;

public class S3FileRetrievalException extends RuntimeException {

    public S3FileRetrievalException(String message) {
        super(message);
    }

    public S3FileRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
