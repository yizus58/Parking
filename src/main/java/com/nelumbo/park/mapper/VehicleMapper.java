package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.dto.response.VehicleCreateResponse;
import com.nelumbo.park.dto.response.VehicleExitResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.exception.exceptions.UserNotFoundException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.repository.ParkingRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class VehicleMapper {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ParkingRepository parkingRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plateNumber", source = "plate_number")
    @Mapping(target = "model", source = "model_vehicle")
    @Mapping(target = "entryTime", source = "entry_time")
    @Mapping(target = "exitTime", source = "exit_time")
    @Mapping(target = "costPerHour", source = "cost_per_hour")
    @Mapping(target = "parking", source = "id_parking", qualifiedByName = "mapParking")
    @Mapping(target = "admin", source = "id_admin", qualifiedByName = "mapAdmin")
    public abstract Vehicle toEntity(VehicleCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plateNumber", source = "plate_number")
    @Mapping(target = "model", source = "model_vehicle")
    @Mapping(target = "entryTime", source = "entry_time")
    @Mapping(target = "exitTime", source = "exit_time")
    @Mapping(target = "costPerHour", source = "cost_per_hour")
    @Mapping(target = "parking", source = "id_parking", qualifiedByName = "mapParking")
    @Mapping(target = "admin", source = "id_admin", qualifiedByName = "mapAdmin")
    public abstract Vehicle toEntity(VehicleUpdateRequest dto);

    public abstract VehicleCreateResponse toSimpleResponse(Vehicle vehicle);

    public VehicleExitResponse toExitResponse(Vehicle vehicle, Date entryTime, Date exitTime, Float totalCost) {
        VehicleExitResponse response = new VehicleExitResponse();
        response.setPlateNumber(vehicle.getPlateNumber());
        response.setModel(vehicle.getModel());
        response.setEntryTime(entryTime);
        response.setExitTime(exitTime);
        response.setCostPerHour(vehicle.getCostPerHour());
        response.setStatus(vehicle.getStatus());
        response.setTotalCost(totalCost);
        return response;
    }

    @Named("mapAdmin")
    protected User mapAdmin(String id_admin) {
        if (id_admin == null) {
            return null;
        }
        return userRepository.findById(id_admin).orElseThrow(() -> new UserNotFoundException());
    }

    @Named("mapParking")
    protected Parking mapParking(String id_parking) {
        if (id_parking == null) {
            return null;
        }
        Parking parking = parkingRepository.findById(id_parking);
        if (parking == null) {
            throw new ParkingNotFoundException("Parking no encontrado con ID: " + id_parking);
        }
        return parking;
    }
}
