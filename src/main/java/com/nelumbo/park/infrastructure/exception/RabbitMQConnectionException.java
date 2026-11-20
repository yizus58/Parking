package com.nelumbo.park.infrastructure.exception;

public class RabbitMQConnectionException extends RuntimeException {

    public RabbitMQConnectionException(String message) {
        super(message);
    }

    public RabbitMQConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
