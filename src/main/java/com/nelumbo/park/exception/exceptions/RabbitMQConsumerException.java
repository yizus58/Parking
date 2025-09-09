package com.nelumbo.park.exception.exceptions;

public class RabbitMQConsumerException extends RuntimeException {

    public RabbitMQConsumerException(String message) {
        super(message);
    }

    public RabbitMQConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}
