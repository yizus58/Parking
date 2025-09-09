package com.nelumbo.park.service;

import com.nelumbo.park.exception.exceptions.S3ConnectivityException;
import com.nelumbo.park.exception.exceptions.S3FileRetrievalException;
import com.nelumbo.park.exception.exceptions.S3FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.UnknownHostException;
import java.util.Map;

@Service
@Slf4j
public class S3Service {

    private S3Client s3Client;

    @Value("${r2.bucket.name}")
    private String bucketName;

    /**
     * Sube un archivo al bucket S3
     * @param buffer Array de bytes del archivo
     * @param contentType Tipo de contenido del archivo
     * @param fileName Nombre del archivo
     * @return Mapa con la clave del archivo subido
     */
    public Map<String, String> uploadFile(byte[] buffer, String contentType, String fileName) {
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("El parámetro buffer debe ser un array de bytes válido");
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(buffer));
            return Map.of("Key", fileName);
        } catch (S3Exception error) {
            throw new S3FileUploadException(String.format("Error de S3 subiendo archivo: %s", error.getMessage()), error);
        } catch (Exception error) {
            if (error.getCause() instanceof UnknownHostException) {
                throw new S3ConnectivityException("Error de conectividad con S3: Verifique la configuración del endpoint y la conectividad de red", error);
            } else {
                throw new S3FileUploadException(String.format("Error subiendo archivo: %s", error.getMessage()), error);
            }
        }
    }

    /**
     * Verifica la existencia de un archivo en el bucket S3
     * @param filename Nombre del archivo a verificar
     * @return Mapa con la clave del archivo y si existe
     */
    public Map<String, Object> getFile(String filename) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            s3Client.getObject(getObjectRequest);

            return Map.of(
                    "key", filename,
                    "exists", true
            );
        } catch (NoSuchKeyException error) {
            return Map.of(
                    "key", filename,
                    "exists", false
            );
        } catch (S3Exception error) {
            throw new S3FileRetrievalException(String.format("Error de S3 verificando archivo: %s", error.getMessage()), error);
        } catch (Exception error) {
            if (error.getCause() instanceof UnknownHostException) {
                throw new S3ConnectivityException("Error de conectividad con S3: Verifique la configuración del endpoint y la conectividad de red", error);
            } else {
                throw new S3FileRetrievalException(String.format("Error verificando existencia del archivo: %s", error.getMessage()), error);
            }
        }
    }
    /**
     * Sube un archivo directamente a S3 sin verificación previa de existencia
     * @param buffer Array de bytes del archivo
     * @param contentType Tipo de contenido del archivo
     * @param fileName Nombre del archivo
     * @return Mapa con la clave del archivo subido
     */
    public Map<String, String> uploadFileDirectly(byte[] buffer, String contentType, String fileName) {
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("El parámetro buffer debe ser un array de bytes válido");
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(buffer));
            return Map.of("Key", fileName);
        } catch (S3Exception error) {
            log.error("Error de S3 subiendo archivo directamente \"{}\": {}", fileName, error.getMessage());
            throw new S3FileUploadException(String.format("Error de S3 subiendo archivo directamente: %s", error.getMessage()), error);
        } catch (Exception error) {
            if (error.getCause() instanceof UnknownHostException) {
                log.error("Error de conectividad subiendo archivo directamente {}: No se puede resolver el host S3", fileName);
                throw new S3ConnectivityException("Error de conectividad con S3: Verifique la configuración del endpoint y la conectividad de red", error);
            } else {
                log.error("Error subiendo archivo directamente \"{}\": {}", fileName, error.getMessage());
                throw new S3FileUploadException(String.format("Error subiendo archivo: %s", error.getMessage()), error);
            }
        }
    }
}
