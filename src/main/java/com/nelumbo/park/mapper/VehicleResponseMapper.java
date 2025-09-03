package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.response.VehicleResponse;
import com.nelumbo.park.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ParkingResponseMapper.class})
public interface VehicleResponseMapper {

    VehicleResponse toResponse(Vehicle vehicle);
    List<VehicleResponse> toResponseList(List<Vehicle> vehicles);
}
