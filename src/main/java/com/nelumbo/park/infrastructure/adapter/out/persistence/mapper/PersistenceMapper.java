package com.nelumbo.park.infrastructure.adapter.out.persistence.mapper;

import com.nelumbo.park.domain.model.LoginLog;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.LoginLogEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.ParkingEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.VehicleEntity;

import java.time.Instant;
import java.util.Date;

/**
 * Converts between JPA entities and domain objects. Pure functions: related
 * entities (owner, parking, admin) are resolved by the adapters and passed in.
 */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(entity.getId(), entity.getUsername(), entity.getPassword(), entity.getEmail(), entity.getRole());
    }

    public static Parking toDomain(ParkingEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Parking(entity.getId(), entity.getName(), entity.getAddress(), entity.getCapacity(),
                entity.getCostPerHour(), toDomain(entity.getOwner()));
    }

    public static Vehicle toDomain(VehicleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Vehicle(entity.getId(), entity.getPlateNumber(), entity.getModel(),
                toInstant(entity.getEntryTime()), toInstant(entity.getExitTime()), entity.getCostPerHour(),
                entity.getStatus(), toDomain(entity.getParking()), toDomain(entity.getAdmin()));
    }

    public static void copy(User source, UserEntity target) {
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setEmail(source.getEmail());
        if (source.getRole() != null) {
            target.setRole(source.getRole());
        }
    }

    public static void copy(Parking source, ParkingEntity target, UserEntity owner) {
        target.setName(source.getName());
        target.setAddress(source.getAddress());
        target.setCapacity(source.getCapacity());
        target.setCostPerHour(source.getCostPerHour());
        target.setOwner(owner);
    }

    public static void copy(Vehicle source, VehicleEntity target, ParkingEntity parking, UserEntity admin) {
        target.setPlateNumber(source.getPlateNumber());
        target.setModel(source.getModel());
        target.setEntryTime(toDate(source.getEntryTime()));
        target.setExitTime(toDate(source.getExitTime()));
        target.setCostPerHour(source.getCostPerHour());
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
        target.setParking(parking);
        target.setAdmin(admin);
    }

    public static LoginLogEntity toEntity(LoginLog loginLog) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setId(loginLog.id());
        entity.setUsername(loginLog.username());
        entity.setEmail(loginLog.email());
        entity.setDescription(loginLog.description());
        entity.setDate(toDate(loginLog.date()));
        return entity;
    }

    public static Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }

    public static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
}
