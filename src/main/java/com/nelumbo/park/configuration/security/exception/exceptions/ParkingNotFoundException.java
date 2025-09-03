package com.nelumbo.park.configuration.security.exception.exceptions;

public class ParkingNotFoundException extends RuntimeException {
    public ParkingNotFoundException() {
        super();
    }

    public ParkingNotFoundException(String message) {
        super(message);
    }
}
