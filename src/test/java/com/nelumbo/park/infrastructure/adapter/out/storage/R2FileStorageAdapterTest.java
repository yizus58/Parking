package com.nelumbo.park.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class R2FileStorageAdapterTest {

    @Test
    void storedWhenUploadReturnsKey() {
        S3Service s3 = mock(S3Service.class);
        byte[] content = {1};
        when(s3.uploadFile(content, "type", "k1")).thenReturn(Map.of("Key", "k1"));
        when(s3.uploadFile(content, "type", "k2")).thenReturn(Map.of());

        R2FileStorageAdapter adapter = new R2FileStorageAdapter(s3);

        assertTrue(adapter.store(content, "type", "k1"));
        assertFalse(adapter.store(content, "type", "k2"));
    }
}
