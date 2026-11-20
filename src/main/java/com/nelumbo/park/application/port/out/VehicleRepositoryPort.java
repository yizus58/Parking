package com.nelumbo.park.application.port.out;

import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.domain.model.stats.TopVehicle;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VehicleRepositoryPort {

    List<Vehicle> findAll();

    Optional<Vehicle> findById(String id);

    List<Vehicle> findByAdminId(String adminId);

    List<Vehicle> findByParkingId(String parkingId);

    Optional<Vehicle> findByPlateNumberAndStatus(String plateNumber, VehicleStatus status);

    long countByParkingIdAndStatus(String parkingId, VehicleStatus status);

    /** Vehicles with the given status whose plate appears once (original query kept as is). */
    List<Vehicle> findFirstTimeParked(VehicleStatus status);

    List<TopVehicle> findTopVehicles(int limit);

    List<TopVehicle> findTopVehiclesByParking(String parkingId, int limit);

    /** Partners (SOCIO) ranked by vehicles entered into their parkings within the period. */
    List<TopPartner> findTopPartners(Instant from, Instant to, int limit);

    List<Vehicle> findExitedBetween(Instant from, Instant to);

    Vehicle save(Vehicle vehicle);

    void deleteById(String id);
}
