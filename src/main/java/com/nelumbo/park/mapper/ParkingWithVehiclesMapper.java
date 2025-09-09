package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.response.ParkingWithVehiclesResponse;
import com.nelumbo.park.dto.response.VehicleSimpleResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class ParkingWithVehiclesMapper {

    public ParkingWithVehiclesResponse toResponse(Parking parking) {
        ParkingWithVehiclesResponse response = new ParkingWithVehiclesResponse();
        response.setId(parking.getId());
        response.setName(parking.getName());
        response.setAddress(parking.getAddress());
        response.setCapacity(parking.getCapacity());
        response.setCostPerHour(parking.getCostPerHour());

        if (parking.getVehicles() != null && !parking.getVehicles().isEmpty()) {
            response.setVehicles(parking.getVehicles().stream()
                    .map(this::toVehicleSimpleResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setVehicles(new ArrayList<>());
        }
        
        return response;
    }

    private VehicleSimpleResponse toVehicleSimpleResponse(Vehicle vehicle) {
        VehicleSimpleResponse response = new VehicleSimpleResponse();
        response.setId(vehicle.getId());
        response.setPlateNumber(vehicle.getPlateNumber());
        response.setModel(vehicle.getModel());
        response.setEntryTime(vehicle.getEntryTime());
        response.setExitTime(vehicle.getExitTime());
        response.setCostPerHour(vehicle.getCostPerHour());
        response.setStatus(vehicle.getStatus());
        return response;
    }
}