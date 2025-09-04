package com.nelumbo.park.configuration.security.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nelumbo.park.configuration.security.exception.exceptions.EmailNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.InvalidPasswordException;
import com.nelumbo.park.configuration.security.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.configuration.security.exception.exceptions.NoAssociatedParkingException;
import com.nelumbo.park.configuration.security.exception.exceptions.UserNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.DuplicateEmailException;
import com.nelumbo.park.configuration.security.exception.exceptions.DuplicateUsernameException;
import com.nelumbo.park.configuration.security.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.configuration.security.exception.exceptions.JwtUserNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.VehicleOutParkingException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        if (errors.isEmpty()) {
            ex.getBindingResult().getGlobalErrors().forEach(error ->
                    errors.put("error", error.getDefaultMessage())
            );
        }

        if (errors.isEmpty()) {
            errors.put("error", "Los datos enviados no son válidos");
            errors.put("message", "Verifique que todos los campos requeridos estén presentes y sean correctos");
        }

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
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> errors = new HashMap<>();

        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("Unexpected character") ||
                    message.contains("was expecting") ||
                    message.contains("Unexpected end-of-input") ||
                    message.contains("not a valid JSON")) {
                return createJsonRequiredResponse();
            }

            if (message.contains("El valor debe ser")) {
                errors.put("error", ex.getCause() != null ? ex.getCause().getMessage() : message);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            if (message.contains("Cannot deserialize") || message.contains("StrictStringDeserializer") ||
                message.contains("StrictIntegerDeserializer") || message.contains("StrictFloatDeserializer")) {
                errors.put("error", "Error en el tipo de datos");
                errors.put("message", "Verifique que todos los campos tengan el tipo de dato correcto (texto, número entero, número decimal)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            if (message.contains("Cannot construct instance")) {
                errors.put("error", "Datos incompatibles con el endpoint");
                errors.put("message", "Verifique que esté usando el endpoint correcto y enviando los campos apropiados");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            if (message.contains("Unrecognized field") || message.contains("not marked as ignorable")) {
                String fieldName = extractFieldNameFromMessage(message);
                errors.put("error", "Campo desconocido: " + fieldName);
                errors.put("message", "El campo '" + fieldName + "' no es válido para este endpoint. Verifique los campos requeridos.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }
        }

        errors.put("error", "Error en el formato de los datos");
        errors.put("message", "Verifique que los datos enviados sean correctos y que esté usando el endpoint apropiado");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return createJsonRequiredResponse();
    }

    private ResponseEntity<Map<String, String>> createJsonRequiredResponse() {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "El cuerpo de la petición es requerido");
        errors.put("message", "Debe enviar un JSON válido con los datos necesarios");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    private String extractFieldNameFromMessage(String message) {
        try {
            if (message.contains("Unrecognized field")) {
                int start = message.indexOf("\"") + 1;
                int end = message.indexOf("\"", start);
                if (start > 0 && end > start) {
                    return message.substring(start, end);
                }
            }
            return "desconocido";
        } catch (Exception e) {
            return "desconocido";
        }
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFormatException(InvalidFormatException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());
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

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEmailNotFoundException(EmailNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("error", "Email no encontrado"));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPasswordException(InvalidPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("error", "Password incorrecta"));
    }

    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientPermissionsException(InsufficientPermissionsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Collections.singletonMap("error", "No tienes permisos para realizar esta acción"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Collections.singletonMap("error", "No tienes permisos para realizar esta acción"));
    }

    @ExceptionHandler(NoAssociatedParkingException.class)
    public ResponseEntity<Map<String, String>> handleNoAssociatedParkingException(NoAssociatedParkingException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("error", "No tienes parkings asociados"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("error", "Usuario referenciado no encontrado"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmailException(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", "El email ya está registrado"));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUsernameException(DuplicateUsernameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", "El nombre de usuario ya está registrado"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Método HTTP no soportado");
        errors.put("message", "El método " + ex.getMethod() + " no está permitido para esta URL. Métodos permitidos: " + String.join(", ", ex.getSupportedMethods()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errors);
    }

    @ExceptionHandler(ParkingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleParkingNotFoundException(ParkingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("error", "El parking no existe"));
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleVehicleNotFoundException(VehicleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("error", "El vehículo no existe"));
    }
    
    @ExceptionHandler(VehicleOutParkingException.class)
    public ResponseEntity<Map<String, String>> handleVehicleOutParkingException(VehicleOutParkingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", "El vehículo ya tiene salida registrada"));
    }

    @ExceptionHandler(VehicleAlreadyInParkingException.class)
    public ResponseEntity<Map<String, String>> handleVehicleAlreadyInParkingException(VehicleAlreadyInParkingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", ex.getMessage()));
    }

    @ExceptionHandler(JwtUserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleJwtUserNotFoundException(JwtUserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("error", "El usuario no existe"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        if (ex.getMessage() != null &&
                (ex.getMessage().toLowerCase().contains("content type") ||
                        (ex.getMessage().toLowerCase().contains("media type") && !ex.getMessage().contains("application/json")))) {
            return createJsonRequiredResponse();
        }

        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Error interno del servidor");
        errors.put("message", "Ha ocurrido un error inesperado");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }

}
