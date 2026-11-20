package com.nelumbo.park.domain.exception;

public class NoAssociatedParkingException extends RuntimeException {
    public NoAssociatedParkingException() {
        super();
    }

    public NoAssociatedParkingException(String message) {
        super(message);
    }
}
