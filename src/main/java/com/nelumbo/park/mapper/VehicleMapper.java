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
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class VehicleMapper {

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ParkingRepository parkingRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plateNumber", source = "plateNumber")
    @Mapping(target = "model", source = "modelVehicle")
    @Mapping(target = "entryTime", source = "entryTime")
    @Mapping(target = "exitTime", source = "exitTime")
    @Mapping(target = "costPerHour", source = "costPerHour")
    @Mapping(target = "parking", source = "idParking", qualifiedByName = "mapParking")
    @Mapping(target = "admin", source = "idAdmin", qualifiedByName = "mapAdmin")
    public abstract Vehicle toEntity(VehicleCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plateNumber", source = "plateNumber")
    @Mapping(target = "model", source = "modelVehicle")
    @Mapping(target = "entryTime", source = "entryTime")
    @Mapping(target = "exitTime", source = "exitTime")
    @Mapping(target = "costPerHour", source = "costPerHour")
    @Mapping(target = "parking", source = "idParking", qualifiedByName = "mapParking")
    @Mapping(target = "admin", source = "idAdmin", qualifiedByName = "mapAdmin")
    public abstract Vehicle toEntity(VehicleUpdateRequest dto);

    public abstract VehicleCreateResponse toSimpleResponse(Vehicle vehicle);

    public VehicleExitResponse toExitResponse(Vehicle vehicle, Date entryTime, Date exitTime, String totalCost) {
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
    protected User mapAdmin(String idAdmin) {
        if (idAdmin == null) {
            return null;
        }
       return userRepository.findById(idAdmin).orElseThrow(UserNotFoundException::new);
    }

    @Named("mapParking")
    protected Parking mapParking(String idParking) {
        if (idParking == null) {
            return null;
        }
        Parking parking = parkingRepository.findById(idParking);
        if (parking == null) {
            throw new ParkingNotFoundException("Parking no encontrado con ID: " + idParking);
        }
        return parking;
    }
}
