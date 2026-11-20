package com.nelumbo.park.domain.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDetail {
    private String vehicleId;
    private String plateNumber;
    private String modelVehicle;
    private String dayEntry;
    private String dayExit;
    private Float totalCost;
}