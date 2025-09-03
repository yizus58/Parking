package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.configuration.security.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.configuration.security.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.mapper.VehicleMapper;
import com.nelumbo.park.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final SecurityService securityService;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VehicleMapper vehicleMapper,
            SecurityService securityService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.securityService = securityService;
    }

    public List<Vehicle> getAllVehicles() {
        User currentUser = securityService.getCurrentUser();

        if (securityService.isAdmin()) {
            return vehicleRepository.findAll();
        } else if (securityService.isEmpleado()) {
            return vehicleRepository.findByAdmin(currentUser);
        }

        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(String id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new VehicleNotFoundException());

        User currentUser = securityService.getCurrentUser();

        if (securityService.isEmpleado()) {
            if (!vehicle.getAdmin().getId().equals(currentUser.getId())) {
                throw new InsufficientPermissionsException();
            }
        }

        return vehicle;
    }

    public Vehicle createVehicle(VehicleCreateRequest vehicleCreateRequest) {
        Vehicle vehicle = vehicleMapper.toEntity(vehicleCreateRequest);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(String id, VehicleUpdateRequest vehicleUpdateRequest) {
        Vehicle existingVehicle = vehicleRepository.findById(id).orElseThrow(() -> new VehicleNotFoundException());

        User currentUser = securityService.getCurrentUser();

        if (securityService.isEmpleado()) {
            if (!existingVehicle.getAdmin().getId().equals(currentUser.getId())) {
                throw new InsufficientPermissionsException();
            }
        }

        Vehicle updatedVehicle = vehicleMapper.toEntity(vehicleUpdateRequest);
        updatedVehicle.setId(id);

        return vehicleRepository.save(updatedVehicle);
    }

    public void deleteVehicle(String id) {
        Vehicle existingVehicle = vehicleRepository.findById(id).orElseThrow(() -> new VehicleNotFoundException());

        vehicleRepository.delete(existingVehicle);
    }
}
