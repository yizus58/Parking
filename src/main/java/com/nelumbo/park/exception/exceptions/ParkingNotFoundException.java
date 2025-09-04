package com.nelumbo.park.exception.exceptions;

public class ParkingNotFoundException extends RuntimeException {
    public ParkingNotFoundException() {
        super();
    }

    public ParkingNotFoundException(String message) {
        super(message);
    }
}
