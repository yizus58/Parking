package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.UserNotFoundException;
import com.nelumbo.park.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
@RequiredArgsConstructor
public abstract class ParkingMapper {

    protected final UserRepository userRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "idOwner", qualifiedByName = "mapOwner")
    @Mapping(target = "costPerHour", source = "costPerHour")
    @Mapping(target = "vehicles", ignore = true)
    public abstract Parking toEntity(ParkingRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "idOwner", qualifiedByName = "mapOwner")
    @Mapping(target = "costPerHour", source = "costPerHour")
    @Mapping(target = "vehicles", ignore = true)
    public abstract Parking toEntity(ParkingUpdateRequest dto);

    @Named("mapOwner")
    protected User mapOwner(String idOwner) {
        if (idOwner == null) {
            return null;
        }
        return userRepository.findById(idOwner).orElseThrow(UserNotFoundException::new);
    }
}
