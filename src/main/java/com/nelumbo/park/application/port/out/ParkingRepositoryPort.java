package com.nelumbo.park.application.port.out;

import com.nelumbo.park.domain.model.Parking;

import java.util.List;
import java.util.Optional;

public interface ParkingRepositoryPort {

    List<Parking> findAll();

    Optional<Parking> findById(String id);

    /**
     * Loads the parking and locks its row until the current transaction ends,
     * so concurrent vehicle entries into the same parking are serialized.
     * Must be called inside a transaction.
     */
    Optional<Parking> findByIdForUpdate(String id);

    List<Parking> findByOwnerId(String ownerId);

    Parking save(Parking parking);

    /** Deletes the parking and, as before, its vehicles. */
    void deleteById(String id);
}
