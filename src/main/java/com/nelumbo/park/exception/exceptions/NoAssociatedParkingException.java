package com.nelumbo.park.exception.exceptions;

public class NoAssociatedParkingException extends RuntimeException {
    public NoAssociatedParkingException() {
        super();
    }

    public NoAssociatedParkingException(String message) {
        super(message);
    }
}
