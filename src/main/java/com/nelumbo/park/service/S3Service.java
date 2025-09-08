package com.nelumbo.park.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;

@Service
@Slf4j
public class S3Service {

    @Autowired
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
        getFile(fileName);

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

            log.info("Archivo subido exitosamente: {}", fileName);
            return Map.of("Key", fileName);
        } catch (Exception error) {
            log.error("Error subiendo archivo \"{}\": {}", fileName, error.getMessage());
            throw new RuntimeException("Error subiendo archivo: " + error.getMessage(), error);
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
        } catch (Exception error) {
            log.error("Error verificando existencia del archivo: {}", error.getMessage());
            throw new RuntimeException("Error verificando existencia del archivo: " + error.getMessage(), error);
        }
    }
}
