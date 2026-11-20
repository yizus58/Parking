package com.nelumbo.park.domain.exception;

/** Login failed. Deliberately the same for an unknown email and a wrong password (no user enumeration). */
public class InvalidCredentialsException extends RuntimeException {
    public static final String MESSAGE = "Credenciales incorrectas";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
