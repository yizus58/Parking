package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoResponse {
    private String nameFile;
    private String s3Name;
}