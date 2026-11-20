package com.nelumbo.park.infrastructure.adapter.out.storage;

import com.nelumbo.park.application.port.out.FileStoragePort;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stores report files in Cloudflare R2 through the S3 API.
 */
@Component
public class R2FileStorageAdapter implements FileStoragePort {

    private final S3Service s3Service;

    public R2FileStorageAdapter(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Override
    public boolean store(byte[] content, String contentType, String key) {
        Map<String, String> result = s3Service.uploadFile(content, contentType, key);
        return result != null && result.containsKey("Key");
    }
}
