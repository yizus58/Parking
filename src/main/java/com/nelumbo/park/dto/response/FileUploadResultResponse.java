package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResultResponse {
    private String idUser;
    private String email;
    private List<FileInfoResponse> files;
}
