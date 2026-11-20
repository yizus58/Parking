package com.nelumbo.park.domain.exception;

public class VehicleAlreadyInParkingException extends RuntimeException {
    public VehicleAlreadyInParkingException() {
        super("El vehiculo ya está registrado y actualmente en un parking");
    }

    public VehicleAlreadyInParkingException(String message) {
        super(message);
    }

    public static VehicleAlreadyInParkingException forPlate(String plateNumber) {
        return new VehicleAlreadyInParkingException(
                "El vehiculo con placa " + plateNumber + " ya está registrado y actualmente en un parking");
    }
}
