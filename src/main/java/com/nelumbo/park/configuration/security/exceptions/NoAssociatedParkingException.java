package com.nelumbo.park.configuration.security.exceptions;

public class NoAssociatedParkingException extends RuntimeException {
    public NoAssociatedParkingException() {
        super();
    }

    public NoAssociatedParkingException(String message) {
        super(message);
    }
}
