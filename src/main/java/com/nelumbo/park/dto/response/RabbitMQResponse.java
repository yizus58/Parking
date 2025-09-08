package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RabbitMQResponse {

    private String type;
    private List<EmailDataResponse> data;

    public RabbitMQResponse(String type, EmailDataResponse singleData) {
        this.type = type;
        this.data = List.of(singleData);
    }
}
