package com.nelumbo.park.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopParkingResponse {
    private String parkingId;
    private String parkingName;
    private Long totalVehicles;
    private String totalCost;
}
