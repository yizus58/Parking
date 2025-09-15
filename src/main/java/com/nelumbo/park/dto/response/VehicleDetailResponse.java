package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDetailResponse {
    private String vehicleId;
    private String plateNumber;
    private String modelVehicle;
    private String dayEntry;
    private String dayExit;
    private Float totalCost;
}