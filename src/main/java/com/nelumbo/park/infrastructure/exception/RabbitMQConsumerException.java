package com.nelumbo.park.infrastructure.exception;

public class RabbitMQConsumerException extends RuntimeException {

    public RabbitMQConsumerException(String message) {
        super(message);
    }

    public RabbitMQConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}
