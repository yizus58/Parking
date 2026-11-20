package com.nelumbo.park.domain.exception;

public class VehicleOutParkingException extends RuntimeException {
    public VehicleOutParkingException() {
        super();
    }

    public VehicleOutParkingException(String message) {
        super(message);
    }
}
