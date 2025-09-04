package com.nelumbo.park.configuration.security.exception.exceptions;

public class VehicleAlreadyInParkingException extends RuntimeException {
    public VehicleAlreadyInParkingException() {
        super("El vehiculo ya está registrado y actualmente en un parking");
    }
    
    public VehicleAlreadyInParkingException(String message) {
        super(message);
    }
}