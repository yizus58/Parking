package com.nelumbo.park.dto.response;

import com.nelumbo.park.enums.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleSimpleResponse {
    private String id;
    private String plateNumber;
    private String model;
    private Date entryTime;
    private Date exitTime;
    private Float costPerHour;
    private VehicleStatus status;
}