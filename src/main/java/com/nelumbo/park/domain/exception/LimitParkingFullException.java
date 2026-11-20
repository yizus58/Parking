package com.nelumbo.park.domain.exception;

public class LimitParkingFullException extends RuntimeException {
    public LimitParkingFullException() {
        super("El parqueadero a llegado a su limite, por favor espere a que halla disponibilidad");
    }

    public LimitParkingFullException(String message) {
        super(message);
    }
}
