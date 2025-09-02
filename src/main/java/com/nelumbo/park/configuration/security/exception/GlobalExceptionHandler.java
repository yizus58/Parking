package com.nelumbo.park.configuration.security.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
          .getFieldErrors()
          .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> errors.put(
                cv.getPropertyPath().toString(),
                cv.getMessage()
        ));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMissingRequestBody(HttpMessageNotReadableException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "El cuerpo de la petición es requerido");
        errors.put("message", "Debe enviar un JSON válido con los datos necesarios");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<String> handleSQLException(SQLException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "No hay dato para el identificador solicitado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Método HTTP no soportado");
        errors.put("message", "El método " + ex.getMethod() + " no está permitido para esta URL. Métodos permitidos: " + String.join(", ", ex.getSupportedMethods()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errors);
    }

}
