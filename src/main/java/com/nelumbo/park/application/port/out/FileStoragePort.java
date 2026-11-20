package com.nelumbo.park.application.port.out;

public interface FileStoragePort {

    /**
     * @return true when the object was stored under {@code key}
     */
    boolean store(byte[] content, String contentType, String key);
}
