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

import java.time.Clock;

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
    public Vehicle registerEntry(VehicleEntryCommand command) {
        if (vehicleRepository.findByPlateNumberAndStatus(command.plateNumber(), VehicleStatus.IN).isPresent()) {
            throw new VehicleAlreadyInParkingException(
                    "El vehiculo con placa " + command.plateNumber() + " ya está registrado y actualmente en un parking");
        }
        Parking parking = parkingRepository.findById(command.parkingId())
                .orElseThrow(() -> new ParkingNotFoundException("Parking no encontrado con ID: " + command.parkingId()));
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
}
