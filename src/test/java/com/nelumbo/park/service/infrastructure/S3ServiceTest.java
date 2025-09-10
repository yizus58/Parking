package com.nelumbo.park.service.infrastructure;

import com.nelumbo.park.exception.exceptions.S3ConnectivityException;
import com.nelumbo.park.exception.exceptions.S3FileRetrievalException;
import com.nelumbo.park.exception.exceptions.S3FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.net.UnknownHostException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    private final String BUCKET_NAME = "test-bucket";
    private final String FILE_NAME = "test-file.txt";
    private final String CONTENT_TYPE = "text/plain";
    private final byte[] BUFFER = "test content".getBytes();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET_NAME);
    }

    // --- uploadFile tests ---

    @Test
    @DisplayName("Should upload file successfully")
    void uploadFile_Success() {
        // Given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        Map<String, String> result = s3Service.uploadFile(BUFFER, CONTENT_TYPE, FILE_NAME);

        // Then
        assertNotNull(result);
        assertEquals(FILE_NAME, result.get("Key"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when buffer is null in uploadFile")
    void uploadFile_NullBuffer_ThrowsIllegalArgumentException() {
        // When / Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            s3Service.uploadFile(null, CONTENT_TYPE, FILE_NAME);
        });
        assertEquals("El parámetro buffer debe ser un array de bytes válido", thrown.getMessage());
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when buffer is empty in uploadFile")
    void uploadFile_EmptyBuffer_ThrowsIllegalArgumentException() {
        // When / Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            s3Service.uploadFile(new byte[0], CONTENT_TYPE, FILE_NAME);
        });
        assertEquals("El parámetro buffer debe ser un array de bytes válido", thrown.getMessage());
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("Should throw S3FileUploadException when S3Exception occurs during uploadFile")
    void uploadFile_S3Exception_ThrowsS3FileUploadException() {
        // Given
        String expectedS3ErrorMessage = "S3 error";
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message(expectedS3ErrorMessage).build());

        // When / Then
        S3FileUploadException thrown = assertThrows(S3FileUploadException.class, () -> {
            s3Service.uploadFile(BUFFER, CONTENT_TYPE, FILE_NAME);
        });

        // 1. Aseguramos que el mensaje de la S3FileUploadException es correcto
        assertTrue(thrown.getMessage().contains("Error de S3 subiendo archivo"));

        // 2. Obtenemos la causa de la excepción
        Throwable cause = thrown.getCause();
        assertNotNull(cause); // Aseguramos que la causa no es nula

        // 3. Aseguramos que la causa es una instancia de AwsServiceException (o S3Exception, si más específico)
        assertTrue(cause instanceof AwsServiceException);

        // 4. Verificamos que el mensaje de la causa contiene el mensaje esperado
        assertTrue(cause.getMessage().contains(expectedS3ErrorMessage));
    }

    @Test
    @DisplayName("Should throw S3ConnectivityException when UnknownHostException occurs during uploadFile")
    void uploadFile_UnknownHostException_ThrowsS3ConnectivityException() {
        // Given
        UnknownHostException unknownHostException = new UnknownHostException("Host not found");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException(unknownHostException));

        // When / Then
        S3ConnectivityException thrown = assertThrows(S3ConnectivityException.class, () -> {
            s3Service.uploadFile(BUFFER, CONTENT_TYPE, FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error de conectividad con S3"));
        assertEquals(unknownHostException, thrown.getCause().getCause());
    }

    @Test
    @DisplayName("Should throw S3FileUploadException when generic Exception occurs during uploadFile")
    void uploadFile_GenericException_ThrowsS3FileUploadException() {
        // Given
        RuntimeException genericException = new RuntimeException("Generic error");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(genericException);

        // When / Then
        S3FileUploadException thrown = assertThrows(S3FileUploadException.class, () -> {
            s3Service.uploadFile(BUFFER, CONTENT_TYPE, FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error subiendo archivo"));
        assertEquals(genericException, thrown.getCause());
    }

    // --- getFile tests ---

    @Test
    @DisplayName("Should return exists true when file is found")
    void getFile_Exists_ReturnsTrue() {
        // Given
        // The getObject method returns ResponseInputStream<GetObjectResponse>
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mock(ResponseInputStream.class));

        // When
        Map<String, Object> result = s3Service.getFile(FILE_NAME);

        // Then
        assertNotNull(result);
        assertEquals(FILE_NAME, result.get("key"));
        assertTrue((Boolean) result.get("exists"));
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should return exists false when NoSuchKeyException occurs")
    void getFile_NoSuchKeyException_ReturnsFalse() {
        // Given
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        // When
        Map<String, Object> result = s3Service.getFile(FILE_NAME);

        // Then
        assertNotNull(result);
        assertEquals(FILE_NAME, result.get("key"));
        assertFalse((Boolean) result.get("exists"));
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should throw S3FileRetrievalException when S3Exception occurs during getFile")
    void getFile_S3Exception_ThrowsS3FileRetrievalException() {
        // Given
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        // When / Then
        S3FileRetrievalException thrown = assertThrows(S3FileRetrievalException.class, () -> {
            s3Service.getFile(FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error de S3 verificando archivo"));
        assertTrue(thrown.getCause() instanceof S3Exception);
        assertEquals("S3 error", thrown.getCause().getMessage());
    }

    @Test
    @DisplayName("Should throw S3ConnectivityException when UnknownHostException occurs during getFile")
    void getFile_UnknownHostException_ThrowsS3ConnectivityException() {
        // Given
        UnknownHostException unknownHostException = new UnknownHostException("Host not found");
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException(unknownHostException));

        // When / Then
        S3ConnectivityException thrown = assertThrows(S3ConnectivityException.class, () -> {
            s3Service.getFile(FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error de conectividad con S3"));
        assertEquals(unknownHostException, thrown.getCause().getCause());
    }

    @Test
    @DisplayName("Should throw S3FileRetrievalException when generic Exception occurs during getFile")
    void getFile_GenericException_ThrowsS3FileRetrievalException() {
        // Given
        RuntimeException genericException = new RuntimeException("Generic error");
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(genericException);

        // When / Then
        S3FileRetrievalException thrown = assertThrows(S3FileRetrievalException.class, () -> {
            s3Service.getFile(FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error verificando existencia del archivo"));
        assertEquals(genericException, thrown.getCause());
    }

    // --- uploadFileDirectly tests ---

    @Test
    @DisplayName("Should upload file directly successfully")
    void uploadFileDirectly_Success() {
        // Given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        Map<String, String> result = s3Service.uploadFileDirectly(BUFFER, CONTENT_TYPE, FILE_NAME);

        // Then
        assertNotNull(result);
        assertEquals(FILE_NAME, result.get("Key"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when buffer is null in uploadFileDirectly")
    void uploadFileDirectly_NullBuffer_ThrowsIllegalArgumentException() {
        // When / Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            s3Service.uploadFileDirectly(null, CONTENT_TYPE, FILE_NAME);
        });
        assertEquals("El parámetro buffer debe ser un array de bytes válido", thrown.getMessage());
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when buffer is empty in uploadFileDirectly")
    void uploadFileDirectly_EmptyBuffer_ThrowsIllegalArgumentException() {
        // When / Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            s3Service.uploadFileDirectly(new byte[0], CONTENT_TYPE, FILE_NAME);
        });
        assertEquals("El parámetro buffer debe ser un array de bytes válido", thrown.getMessage());
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("Should throw S3FileUploadException when S3Exception occurs during uploadFileDirectly")
    void uploadFileDirectly_S3Exception_ThrowsS3FileUploadException() {
        // Given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        // When / Then
        S3FileUploadException thrown = assertThrows(S3FileUploadException.class, () -> {
            s3Service.uploadFileDirectly(BUFFER, CONTENT_TYPE, FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error de S3 subiendo archivo directamente"));
        assertTrue(thrown.getCause() instanceof S3Exception);
        assertEquals("S3 error", thrown.getCause().getMessage());
    }

    @Test
    @DisplayName("Should throw S3ConnectivityException when UnknownHostException occurs during uploadFileDirectly")
    void uploadFileDirectly_UnknownHostException_ThrowsS3ConnectivityException() {
        // Given
        UnknownHostException unknownHostException = new UnknownHostException("Host not found");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException(unknownHostException));

        // When / Then
        S3ConnectivityException thrown = assertThrows(S3ConnectivityException.class, () -> {
            s3Service.uploadFileDirectly(BUFFER, CONTENT_TYPE, FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error de conectividad con S3"));
        assertEquals(unknownHostException, thrown.getCause().getCause());
    }

    @Test
    @DisplayName("Should throw S3FileUploadException when generic Exception occurs during uploadFileDirectly")
    void uploadFileDirectly_GenericException_ThrowsS3FileUploadException() {
        // Given
        RuntimeException genericException = new RuntimeException("Generic error");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(genericException);

        // When / Then
        S3FileUploadException thrown = assertThrows(S3FileUploadException.class, () -> {
            s3Service.uploadFileDirectly(BUFFER, CONTENT_TYPE, FILE_NAME);
        });
        assertTrue(thrown.getMessage().contains("Error subiendo archivo"));
        assertEquals(genericException, thrown.getCause());
    }
}
