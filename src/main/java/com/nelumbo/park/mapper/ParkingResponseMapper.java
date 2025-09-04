package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParkingResponseMapper {

    @Named("toResponseWithId")
    ParkingResponse toResponse(Parking parking);

    @IterableMapping(qualifiedByName = "toResponseWithId")
    List<ParkingResponse> toResponseList(List<Parking> parkings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "owner", qualifiedByName = "toUserResponseWithoutId")
    @Named("toCreateResponseWithoutId")
    ParkingResponse toCreateResponse(Parking parking);

    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Named("toUserResponseWithoutId")
    UserResponse toUserResponseWithoutId(User user);
}
