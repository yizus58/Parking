package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.ParkingRequest;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.repository.UserRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ParkingMapper {

    @Autowired
    protected UserRepository userRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "id_owner", qualifiedByName = "mapOwner")
    public abstract Parking toEntity(ParkingRequest dto);

    @Named("mapOwner")
    protected User mapOwner(String id_owner) {
        if (id_owner == null) {
            return null;
        }
        return userRepository.findById(id_owner).orElse(null);
    }
}
