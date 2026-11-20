package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.ManageParkingsUseCase;
import com.nelumbo.park.application.port.out.ParkingRepositoryPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.NoAssociatedParkingException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.UserNotFoundException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParkingManagementService implements ManageParkingsUseCase {

    private final ParkingRepositoryPort parkingRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final UserRepositoryPort userRepository;
    private final CurrentUserResolver currentUser;

    public ParkingManagementService(ParkingRepositoryPort parkingRepository,
                                    VehicleRepositoryPort vehicleRepository,
                                    UserRepositoryPort userRepository,
                                    CurrentUserResolver currentUser) {
        this.parkingRepository = parkingRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public VisibleParkings listParkings() {
        User user = currentUser.require();
        if (user.isPartner()) {
            List<Parking> own = parkingRepository.findByOwnerId(user.getId());
            if (own.isEmpty()) {
                throw new NoAssociatedParkingException();
            }
            return new VisibleParkings(own, false);
        }
        List<Parking> all = parkingRepository.findAll();
        if (all.isEmpty()) {
            throw new ParkingNotFoundException();
        }
        return new VisibleParkings(all, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingDetails getParking(String id) {
        User user = currentUser.require();
        Parking parking = parkingRepository.findById(id).orElseThrow(ParkingNotFoundException::new);
        if (user.isPartner() && !parking.isOwnedBy(user)) {
            throw new InsufficientPermissionsException();
        }
        return new ParkingDetails(parking, vehicleRepository.findByParkingId(id));
    }

    @Override
    @Transactional
    public Parking createParking(ParkingCommand command) {
        return parkingRepository.save(toParking(null, command));
    }

    @Override
    @Transactional
    public Parking updateParking(String id, ParkingCommand command) {
        parkingRepository.findById(id).orElseThrow(ParkingNotFoundException::new);
        return parkingRepository.save(toParking(id, command));
    }

    @Override
    @Transactional
    public void deleteParking(String id) {
        parkingRepository.findById(id).orElseThrow(ParkingNotFoundException::new);
        parkingRepository.deleteById(id);
    }

    private Parking toParking(String id, ParkingCommand command) {
        User owner = userRepository.findById(command.ownerId()).orElseThrow(UserNotFoundException::new);
        return new Parking(id, command.name(), command.address(), command.capacity(), command.costPerHour(), owner);
    }
}
