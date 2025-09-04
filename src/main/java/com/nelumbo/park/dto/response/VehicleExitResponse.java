package com.nelumbo.park.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nelumbo.park.enums.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleExitResponse {
    private String plateNumber;
    private String model;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm", timezone = "America/Bogota")
    private Date entryTime;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm", timezone = "America/Bogota")
    private Date exitTime;

    private Float costPerHour;
    private VehicleStatus status;
    private Float totalCost;
}