package com.nelumbo.park.configuration.security.exception.exceptions;

public class NoAssociatedParkingException extends RuntimeException {
    public NoAssociatedParkingException() {
        super();
    }

    public NoAssociatedParkingException(String message) {
        super(message);
    }
}
