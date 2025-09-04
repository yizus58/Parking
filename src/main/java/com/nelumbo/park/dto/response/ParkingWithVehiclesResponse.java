package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParkingWithVehiclesResponse {
    private String id;
    private String name;
    private String address;
    private int capacity;
    private Float costPerHour;
    private List<VehicleSimpleResponse> vehicles;
}