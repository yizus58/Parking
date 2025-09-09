package com.nelumbo.park.exception.exceptions;

public class S3FileUploadException extends RuntimeException {

    public S3FileUploadException(String message) {
        super(message);
    }

    public S3FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
