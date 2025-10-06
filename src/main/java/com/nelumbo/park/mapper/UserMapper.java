package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.request.UserCreateRequest;
import com.nelumbo.park.dto.response.UserResponse;
import com.nelumbo.park.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "SOCIO")
    @Mapping(target = "ownedParkings", ignore = true)
    public abstract User toEntity(UserCreateRequest dto);
    
    public abstract UserResponse toResponse(User user);
}