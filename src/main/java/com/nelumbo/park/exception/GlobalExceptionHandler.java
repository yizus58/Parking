package com.nelumbo.park.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nelumbo.park.exception.exceptions.EmailNotFoundException;
import com.nelumbo.park.exception.exceptions.InvalidPasswordException;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.NoAssociatedParkingException;
import com.nelumbo.park.exception.exceptions.UserNotFoundException;
import com.nelumbo.park.exception.exceptions.DuplicateEmailException;
import com.nelumbo.park.exception.exceptions.DuplicateUsernameException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.exception.exceptions.JwtUserNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleOutParkingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_KEY = "error";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        if (errors.isEmpty()) {
            ex.getBindingResult().getGlobalErrors().forEach(error ->
                    errors.put(ERROR_KEY, error.getDefaultMessage())
            );
        }

        if (errors.isEmpty()) {
            errors.put(ERROR_KEY, "Los datos enviados no son válidos");
            errors.put(MESSAGE_KEY, "Verifique que todos los campos requeridos estén presentes y sean correctos");
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

            if (message.contains("Cannot deserialize")) {
                errors.put(ERROR_KEY, "Error en el tipo de datos");
                errors.put(MESSAGE_KEY, "Verifique que todos los campos tengan el tipo de dato correcto (texto, número entero, número decimal)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            if (message.contains("Cannot construct instance")) {
                errors.put(ERROR_KEY, "Datos incompatibles con el endpoint");
                errors.put(MESSAGE_KEY, "Verifique que esté usando el endpoint correcto y enviando los campos apropiados");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            if (message.contains("Unrecognized field") || message.contains("not marked as ignorable")) {
                String fieldName = extractFieldNameFromMessage(message);
                errors.put(ERROR_KEY, "Campo desconocido: " + fieldName);
                errors.put(MESSAGE_KEY, "El campo '" + fieldName + "' no es válido para este endpoint. Verifique los campos requeridos.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }
        }

        errors.put(ERROR_KEY, "Error en el formato de los datos");
        errors.put(MESSAGE_KEY, "Verifique que los datos enviados sean correctos y que esté usando el endpoint apropiado");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return createJsonRequiredResponse();
    }

    private ResponseEntity<Map<String, String>> createJsonRequiredResponse() {
        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, "El cuerpo de la petición es requerido");
        errors.put(MESSAGE_KEY, "Debe enviar un JSON válido con los datos necesarios");
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
        } catch (StringIndexOutOfBoundsException e) {
            logger.warn("Error extrayendo nombre de campo del mensaje: {}", message);
            return "desconocido";
        }
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFormatException(InvalidFormatException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<String> handleSQLException(SQLException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, "No hay dato para el identificador solicitado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEmailNotFoundException(EmailNotFoundException ex) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Email no encontrado");
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPasswordException(InvalidPasswordException ex) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Password incorrecta");
    }

    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientPermissionsException(InsufficientPermissionsException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción");
    }

    @ExceptionHandler(NoAssociatedParkingException.class)
    public ResponseEntity<Map<String, String>> handleNoAssociatedParkingException(NoAssociatedParkingException ex) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "No tienes parkings asociados");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
        return createErrorResponse(HttpStatus.NOT_FOUND, "Usuario referenciado no encontrado");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmailException(DuplicateEmailException ex) {
        return createErrorResponse(HttpStatus.CONFLICT, "El email ya está registrado");
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUsernameException(DuplicateUsernameException ex) {
        return createErrorResponse(HttpStatus.CONFLICT, "El nombre de usuario ya está registrado");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, "Método HTTP no soportado");
        errors.put(MESSAGE_KEY, "El método " + ex.getMethod() + " no está permitido para esta URL. Métodos permitidos: " + String.join(", ", ex.getSupportedMethods()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errors);
    }

    @ExceptionHandler(ParkingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleParkingNotFoundException(ParkingNotFoundException ex) {
        return createErrorResponse(HttpStatus.NOT_FOUND, "El parking no existe");
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleVehicleNotFoundException(VehicleNotFoundException ex) {
        return createErrorResponse(HttpStatus.NOT_FOUND, "El vehículo no existe");
    }

    @ExceptionHandler(VehicleOutParkingException.class)
    public ResponseEntity<Map<String, String>> handleVehicleOutParkingException(VehicleOutParkingException ex) {
        return createErrorResponse(HttpStatus.CONFLICT, "El vehículo ya tiene salida registrada");
    }

    @ExceptionHandler(VehicleAlreadyInParkingException.class)
    public ResponseEntity<Map<String, String>> handleVehicleAlreadyInParkingException(VehicleAlreadyInParkingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(JwtUserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleJwtUserNotFoundException(JwtUserNotFoundException ex) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "El usuario no existe");
    }

    private ResponseEntity<Map<String, String>> createErrorResponse(HttpStatus status, String errorMessage) {
        return ResponseEntity.status(status)
                .body(Collections.singletonMap(ERROR_KEY, errorMessage));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception: ", ex);

        if (ex.getMessage() != null &&
                (ex.getMessage().toLowerCase().contains("content type") ||
                        (ex.getMessage().toLowerCase().contains("media type") && !ex.getMessage().contains("application/json")))) {
            return createJsonRequiredResponse();
        }

        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, "Error de aplicación");
        errors.put(MESSAGE_KEY, "Ha ocurrido un error durante el procesamiento");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Error inesperado no categorizado: ", ex);

        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR_KEY, "Error interno del servidor");
        errors.put(MESSAGE_KEY, "Ha ocurrido un error inesperado del sistema");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }

}
