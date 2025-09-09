package com.nelumbo.park.exception.exceptions;

public class S3FileRetrievalException extends RuntimeException {

    public S3FileRetrievalException(String message) {
        super(message);
    }

    public S3FileRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
