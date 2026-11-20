package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.RegisterVehicleExitUseCase;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.VehicleNotFoundException;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class VehicleExitService implements RegisterVehicleExitUseCase {

    private final VehicleRepositoryPort vehicleRepository;
    private final CurrentUserResolver currentUser;
    private final Clock clock;

    public VehicleExitService(VehicleRepositoryPort vehicleRepository, CurrentUserResolver currentUser, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VehicleExit registerExit(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumberAndStatus(plateNumber.toUpperCase(), VehicleStatus.IN)
                .orElseThrow(VehicleNotFoundException::new);
        Optional<User> user = currentUser.current();
        if (currentUser.isPartner(user) && !vehicle.wasRegisteredBy(user.get())) {
            throw new InsufficientPermissionsException();
        }
        if (vehicle.getParking() == null) {
            throw new ParkingNotFoundException();
        }
        float totalCost = vehicle.exit(clock.instant());
        return new VehicleExit(vehicleRepository.save(vehicle), totalCost);
    }
}
