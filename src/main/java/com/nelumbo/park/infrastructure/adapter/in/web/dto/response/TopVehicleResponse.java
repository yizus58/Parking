package com.nelumbo.park.infrastructure.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopVehicleResponse {
    private String plateNumber;
    private Long totalVisits;
}