package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParkingResponseMapper {

    ParkingResponse toResponse(Parking parking);
    List<ParkingResponse> toResponseList(List<Parking> parkings);

    UserResponse toUserResponse(User user);
}
