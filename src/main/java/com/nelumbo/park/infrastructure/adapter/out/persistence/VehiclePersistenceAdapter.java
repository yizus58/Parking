package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.domain.model.stats.TopVehicle;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.ParkingEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.VehicleEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.ParkingJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.VehicleJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.nelumbo.park.infrastructure.adapter.out.persistence.mapper.PersistenceMapper.toDate;

@Component
public class VehiclePersistenceAdapter implements VehicleRepositoryPort {

    /** Partial unique index on plate_number WHERE status = 'IN' (migration V2). */
    static final String ACTIVE_PLATE_INDEX = "ux_vehicles_plate_inside";

    private final VehicleJpaRepository vehicleRepository;
    private final ParkingJpaRepository parkingRepository;
    private final UserJpaRepository userRepository;

    public VehiclePersistenceAdapter(VehicleJpaRepository vehicleRepository,
                                     ParkingJpaRepository parkingRepository,
                                     UserJpaRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.parkingRepository = parkingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Vehicle> findAll() {
        return toDomain(vehicleRepository.findAll());
    }

    @Override
    public Optional<Vehicle> findById(String id) {
        return vehicleRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Vehicle> findByAdminId(String adminId) {
        return toDomain(vehicleRepository.findByAdminId(adminId));
    }

    @Override
    public List<Vehicle> findByParkingId(String parkingId) {
        return toDomain(vehicleRepository.findByParkingId(parkingId));
    }

    @Override
    public Optional<Vehicle> findByPlateNumberAndStatus(String plateNumber, VehicleStatus status) {
        return vehicleRepository.findFirstByPlateNumberAndStatus(plateNumber, status).map(PersistenceMapper::toDomain);
    }

    @Override
    public long countByParkingIdAndStatus(String parkingId, VehicleStatus status) {
        return vehicleRepository.countByParkingIdAndStatus(parkingId, status);
    }

    @Override
    public List<Vehicle> findFirstTimeParked(VehicleStatus status) {
        return toDomain(vehicleRepository.findFirstTimeParkedVehicles(status));
    }

    @Override
    public List<TopVehicle> findTopVehicles(int limit) {
        return toTopVehicles(vehicleRepository.findTopVehiclesByVisits(PageRequest.of(0, limit)));
    }

    @Override
    public List<TopVehicle> findTopVehiclesByParking(String parkingId, int limit) {
        return toTopVehicles(vehicleRepository.findTopVehiclesByParking(parkingId, PageRequest.of(0, limit)));
    }

    @Override
    public List<TopPartner> findTopPartners(Instant from, Instant to, int limit) {
        return vehicleRepository.findTopPartners(toDate(from), toDate(to), PageRequest.of(0, limit));
    }

    @Override
    public List<Vehicle> findExitedBetween(Instant from, Instant to) {
        return toDomain(vehicleRepository.findExitedBetween(toDate(from), toDate(to)));
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        ParkingEntity parking = vehicle.getParking() != null && vehicle.getParking().getId() != null
                ? parkingRepository.getReferenceById(vehicle.getParking().getId())
                : null;
        UserEntity admin = vehicle.getAdmin() != null && vehicle.getAdmin().getId() != null
                ? userRepository.getReferenceById(vehicle.getAdmin().getId())
                : null;
        VehicleEntity entity = vehicle.getId() != null
                ? vehicleRepository.findById(vehicle.getId()).orElseGet(VehicleEntity::new)
                : new VehicleEntity();
        PersistenceMapper.copy(vehicle, entity, parking, admin);
        VehicleEntity saved;
        try {
            // Flush now so a unique-index violation surfaces here, as a domain error,
            // instead of at commit time as a generic data access exception.
            saved = vehicleRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (violates(e, ACTIVE_PLATE_INDEX)) {
                throw VehicleAlreadyInParkingException.forPlate(vehicle.getPlateNumber());
            }
            throw e;
        }
        Vehicle result = PersistenceMapper.toDomain(saved);
        // Keep the already loaded aggregates instead of touching lazy references.
        result.setParking(vehicle.getParking());
        result.setAdmin(vehicle.getAdmin());
        return result;
    }

    @Override
    public void deleteById(String id) {
        vehicleRepository.deleteById(id);
    }

    static boolean violates(Throwable error, String constraintName) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null
                    && cve.getConstraintName().equalsIgnoreCase(constraintName)) {
                return true;
            }
            if (t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains(constraintName)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private static List<Vehicle> toDomain(List<VehicleEntity> entities) {
        return entities.stream().map(PersistenceMapper::toDomain).toList();
    }

    private static List<TopVehicle> toTopVehicles(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TopVehicle((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
