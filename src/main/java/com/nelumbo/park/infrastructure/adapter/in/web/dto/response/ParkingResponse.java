package com.nelumbo.park.infrastructure.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParkingResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    private String name;
    private String address;
    private int capacity;
    private Float costPerHour;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserResponse owner;
}
