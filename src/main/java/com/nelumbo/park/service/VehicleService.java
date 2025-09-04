package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.dto.response.VehicleSimpleResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.configuration.security.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.configuration.security.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.configuration.security.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.mapper.VehicleMapper;
import com.nelumbo.park.repository.VehicleRepository;
import com.nelumbo.park.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final SecurityService securityService;
    private final UserRepository userRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VehicleMapper vehicleMapper,
            SecurityService securityService,
            UserRepository userRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.securityService = securityService;
        this.userRepository = userRepository;
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

    public VehicleSimpleResponse createVehicle(VehicleCreateRequest vehicleCreateRequest) {
        Vehicle vehicle = vehicleMapper.toEntity(vehicleCreateRequest);

        vehicleRepository.findByPlateNumberAndStatus(vehicle.getPlateNumber(), VehicleStatus.IN)
                .ifPresent(existingVehicle -> {
                    throw new VehicleAlreadyInParkingException(
                            "El vehiculo con placa " + vehicle.getPlateNumber() + 
                            " ya está registrado y actualmente en un parking"
                    );
                });

        User currentUser = securityService.getCurrentUser();
        String userId = currentUser.getId();

        vehicle.setCostPerHour(vehicle.getParking().getCostPerHour());

        User admin = userRepository.findById(userId).orElse(null);
        Date entryTime = new Date();

        vehicle.setEntryTime(entryTime);
        vehicle.setAdmin(admin);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toSimpleResponse(savedVehicle);
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
