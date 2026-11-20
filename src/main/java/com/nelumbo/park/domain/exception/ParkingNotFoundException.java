package com.nelumbo.park.domain.exception;

public class ParkingNotFoundException extends RuntimeException {
    public ParkingNotFoundException() {
        super();
    }

    public ParkingNotFoundException(String message) {
        super(message);
    }
}
