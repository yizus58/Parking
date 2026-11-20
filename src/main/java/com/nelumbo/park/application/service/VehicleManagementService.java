package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.ManageVehiclesUseCase;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.VehicleNotFoundException;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleManagementService implements ManageVehiclesUseCase {

    private final VehicleRepositoryPort vehicleRepository;
    private final CurrentUserResolver currentUser;

    public VehicleManagementService(VehicleRepositoryPort vehicleRepository, CurrentUserResolver currentUser) {
        this.vehicleRepository = vehicleRepository;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> listVehicles() {
        Optional<User> user = currentUser.current();
        if (user.isPresent() && !user.get().isAdmin() && user.get().isPartner()) {
            return vehicleRepository.findByAdminId(user.get().getId());
        }
        return vehicleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Vehicle getVehicle(String id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(VehicleNotFoundException::new);
        Optional<User> user = currentUser.current();
        if (currentUser.isPartner(user) && !vehicle.wasRegisteredBy(user.get())) {
            throw new InsufficientPermissionsException();
        }
        return vehicle;
    }

    @Override
    @Transactional
    public void deleteVehicle(String id) {
        vehicleRepository.findById(id).orElseThrow(VehicleNotFoundException::new);
        vehicleRepository.deleteById(id);
    }
}
