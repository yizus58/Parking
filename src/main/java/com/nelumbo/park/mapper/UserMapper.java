package com.nelumbo.park.mapper;

import com.nelumbo.park.dto.UserCreateRequest;
import com.nelumbo.park.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "EMPLEADO")
    User toEntity(UserCreateRequest dto);
}