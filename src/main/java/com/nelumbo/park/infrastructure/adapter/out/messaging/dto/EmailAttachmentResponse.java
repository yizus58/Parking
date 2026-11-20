package com.nelumbo.park.infrastructure.adapter.out.messaging.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAttachmentResponse {

    private String nameFile;
    private String s3Name;
}
