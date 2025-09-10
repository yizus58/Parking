package com.nelumbo.park.exception;

import com.nelumbo.park.exception.exceptions.DuplicateEmailException;
import com.nelumbo.park.exception.exceptions.DuplicateUsernameException;
import com.nelumbo.park.exception.exceptions.EmailNotFoundException;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.InvalidPasswordException;
import com.nelumbo.park.exception.exceptions.JwtUserNotFoundException;
import com.nelumbo.park.exception.exceptions.NoAssociatedParkingException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.exception.exceptions.UserNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleOutParkingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
    }

    @Test
    void handleSQLException_shouldReturnConflict() {
        SQLException ex = new SQLException("Database constraint violation");

        ResponseEntity<String> response = globalExceptionHandler.handleSQLException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Database constraint violation", response.getBody());
    }

    @Test
    void handleIllegalArgument_shouldReturnUnauthorized() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Argumento inválido", response.getBody().get("error"));
    }

    @Test
    void handleNoSuchElement_shouldReturnNotFound() {
        NoSuchElementException ex = new NoSuchElementException();

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleNoSuchElement(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No hay dato para el identificador solicitado", response.getBody().get("error"));
    }

    @Test
    void handleEmailNotFoundException_shouldReturnUnauthorized() {
        EmailNotFoundException ex = new EmailNotFoundException("Email not found");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleEmailNotFoundException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Email no encontrado", response.getBody().get("error"));
    }

    @Test
    void handleInvalidPasswordException_shouldReturnUnauthorized() {
        InvalidPasswordException ex = new InvalidPasswordException("Invalid password");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleInvalidPasswordException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Password incorrecta", response.getBody().get("error"));
    }

    @Test
    void handleInsufficientPermissionsException_shouldReturnForbidden() {
        InsufficientPermissionsException ex = new InsufficientPermissionsException("Insufficient permissions");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleInsufficientPermissionsException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No tienes permisos para realizar esta acción", response.getBody().get("error"));
    }

    @Test
    void handleAccessDeniedException_shouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No tienes permisos para realizar esta acción", response.getBody().get("error"));
    }

    @Test
    void handleNoAssociatedParkingException_shouldReturnUnauthorized() {
        NoAssociatedParkingException ex = new NoAssociatedParkingException("No parking associated");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleNoAssociatedParkingException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No tienes parkings asociados", response.getBody().get("error"));
    }

    @Test
    void handleUserNotFoundException_shouldReturnNotFound() {
        UserNotFoundException ex = new UserNotFoundException("User not found");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleUserNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Usuario referenciado no encontrado", response.getBody().get("error"));
    }

    @Test
    void handleDuplicateEmailException_shouldReturnConflict() {
        DuplicateEmailException ex = new DuplicateEmailException("Duplicate email");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDuplicateEmailException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El email ya está registrado", response.getBody().get("error"));
    }

    @Test
    void handleDuplicateUsernameException_shouldReturnConflict() {
        DuplicateUsernameException ex = new DuplicateUsernameException("Duplicate username");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleDuplicateUsernameException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El nombre de usuario ya está registrado", response.getBody().get("error"));
    }

    @Test
    void handleMethodNotSupported_shouldReturnMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("DELETE");
        when(ex.getSupportedMethods()).thenReturn(new String[]{"GET", "POST"});

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Método HTTP no soportado", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").contains("DELETE"));
        assertTrue(response.getBody().get("message").contains("GET, POST"));
    }

    @Test
    void handleParkingNotFoundException_shouldReturnNotFound() {
        ParkingNotFoundException ex = new ParkingNotFoundException("Parking not found");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleParkingNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El parking no existe", response.getBody().get("error"));
    }

    @Test
    void handleVehicleNotFoundException_shouldReturnNotFound() {
        VehicleNotFoundException ex = new VehicleNotFoundException("Vehicle not found");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleVehicleNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El vehículo no existe", response.getBody().get("error"));
    }

    @Test
    void handleVehicleOutParkingException_shouldReturnConflict() {
        VehicleOutParkingException ex = new VehicleOutParkingException("Vehicle already out");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleVehicleOutParkingException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El vehículo ya tiene salida registrada", response.getBody().get("error"));
    }

    @Test
    void handleVehicleAlreadyInParkingException_shouldReturnConflict() {
        VehicleAlreadyInParkingException ex = new VehicleAlreadyInParkingException("Vehicle already in parking");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleVehicleAlreadyInParkingException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Vehicle already in parking", response.getBody().get("error"));
    }

    @Test
    void handleJwtUserNotFoundException_shouldReturnUnauthorized() {
        JwtUserNotFoundException ex = new JwtUserNotFoundException("JWT user not found");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleJwtUserNotFoundException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El usuario no existe", response.getBody().get("error"));
    }

    @Test
    void handleRuntimeException_withContentTypeError_shouldReturnJsonRequiredMessage() {
        RuntimeException ex = new RuntimeException("Content type error");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El cuerpo de la petición es requerido", response.getBody().get("error"));
        assertEquals("Debe enviar un JSON válido con los datos necesarios", response.getBody().get("message"));
    }

    @Test
    void handleRuntimeException_withGenericError_shouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("Generic runtime error");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error de aplicación", response.getBody().get("error"));
        assertEquals("Ha ocurrido un error durante el procesamiento", response.getBody().get("message"));
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new Exception("Generic exception");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error interno del servidor", response.getBody().get("error"));
        assertEquals("Ha ocurrido un error inesperado del sistema", response.getBody().get("message"));
    }

    @Test
    void handleHttpMessageNotReadable_withNullMessage_shouldReturnGenericError() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn(null);

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error en el formato de los datos", response.getBody().get("error"));
        assertEquals("Verifique que los datos enviados sean correctos y que esté usando el endpoint apropiado", 
                response.getBody().get("message"));
    }

    @Test
    void handleHttpMessageNotReadable_withConstructInstanceError_shouldReturnDataCompatibilityError() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Cannot construct instance of com.example.User");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Datos incompatibles con el endpoint", response.getBody().get("error"));
        assertEquals("Verifique que esté usando el endpoint correcto y enviando los campos apropiados", 
                response.getBody().get("message"));
    }
}
