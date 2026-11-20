package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase;
import com.nelumbo.park.application.port.out.ParkingRepositoryPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.LimitParkingFullException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Vehicle entry, guarded against exceeding the parking capacity under concurrency.
 *
 * <p>The whole entry runs in one transaction that first locks the parking row
 * (SELECT ... FOR UPDATE). Concurrent entries into the same parking therefore
 * run one after another, and each one counts the vehicles inside only after
 * the previous entry has committed. A pessimistic lock was chosen over
 * optimistic versioning because it needs no schema change (no version column,
 * and ddl-auto no longer alters the schema), needs no retry logic in callers,
 * and the lock is held only for a few short statements on a single row.
 */
@Service
public class VehicleEntryService implements RegisterVehicleEntryUseCase {

    private final VehicleRepositoryPort vehicleRepository;
    private final ParkingRepositoryPort parkingRepository;
    private final CurrentUserResolver currentUser;
    private final Clock clock;

    public VehicleEntryService(VehicleRepositoryPort vehicleRepository,
                               ParkingRepositoryPort parkingRepository,
                               CurrentUserResolver currentUser,
                               Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.parkingRepository = parkingRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Vehicle registerEntry(VehicleEntryCommand command) {
        ensureNotInside(command.plateNumber());
        Parking parking = parkingRepository.findByIdForUpdate(command.parkingId())
                .orElseThrow(() -> new ParkingNotFoundException("Parking no encontrado con ID: " + command.parkingId()));
        // Checked again under the lock: a concurrent entry of the same plate into
        // this parking may have committed while we were waiting.
        ensureNotInside(command.plateNumber());
        User user = currentUser.require();
        if (!parking.isOwnedBy(user)) {
            throw new InsufficientPermissionsException();
        }
        long inside = vehicleRepository.countByParkingIdAndStatus(parking.getId(), VehicleStatus.IN);
        if (parking.isFull(inside)) {
            throw new LimitParkingFullException(
                    "El limite de vehiculos en parking " + parking.getName() + " ya ha sido alcanzado");
        }
        return vehicleRepository.save(Vehicle.enter(command.plateNumber(), command.model(), parking, user, clock.instant()));
    }

    private void ensureNotInside(String plateNumber) {
        if (vehicleRepository.findByPlateNumberAndStatus(plateNumber, VehicleStatus.IN).isPresent()) {
            throw new VehicleAlreadyInParkingException(
                    "El vehiculo con placa " + plateNumber + " ya está registrado y actualmente en un parking");
        }
    }
}
