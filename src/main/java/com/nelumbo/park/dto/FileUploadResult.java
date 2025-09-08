package com.nelumbo.park.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResult {
    private String idUser;
    private String email;
    private FileInfo files;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileInfo {
        private String nameFile;
        private String s3Name;
    }
}
