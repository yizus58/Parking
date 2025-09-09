package com.nelumbo.park.exception.exceptions;

public class RabbitMQMessagePublishException extends RuntimeException {

    public RabbitMQMessagePublishException(String message) {
        super(message);
    }

    public RabbitMQMessagePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
