package com.nelumbo.park.infrastructure.exception;

public class RabbitMQMessagePublishException extends RuntimeException {

    public RabbitMQMessagePublishException(String message) {
        super(message);
    }

    public RabbitMQMessagePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
