package com.nelumbo.park.service;

import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleOutParkingException;
import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.dto.response.VehicleCreateResponse;
import com.nelumbo.park.dto.response.VehicleExitResponse;
import com.nelumbo.park.dto.response.IndicatorResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.mapper.VehicleMapper;
import com.nelumbo.park.repository.VehicleRepository;
import com.nelumbo.park.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
        } else if (securityService.isSocio()) {
            return vehicleRepository.findByAdmin(currentUser);
        }

        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(String id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new VehicleNotFoundException());

        User currentUser = securityService.getCurrentUser();

        if (securityService.isSocio()) {
            if (!vehicle.getAdmin().getId().equals(currentUser.getId())) {
                throw new InsufficientPermissionsException();
            }
        }

        return vehicle;
    }

    public VehicleCreateResponse createVehicle(VehicleCreateRequest vehicleCreateRequest) {
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

    public VehicleExitResponse exitVehicle(VehicleUpdateRequest vehicleUpdateRequest) {
        String vehiclePlate = vehicleUpdateRequest.getPlateNumber().toUpperCase();
        Vehicle existingVehicle = vehicleRepository.findByPlateNumberAndStatus(vehiclePlate, VehicleStatus.IN)
                .orElseThrow(() -> new VehicleNotFoundException());

        User currentUser = securityService.getCurrentUser();

        if (securityService.isSocio()) {
            if (!existingVehicle.getAdmin().getId().equals(currentUser.getId())) {
                throw new InsufficientPermissionsException();
            }
        }

        Parking parking = existingVehicle.getParking();
        if (parking == null) {
            throw new ParkingNotFoundException();
        }

        if (existingVehicle.getStatus() != VehicleStatus.IN) {
            throw new VehicleOutParkingException();
        }

        Date currentTime = new Date();
        Date exitTime = new Date(currentTime.getTime() - (5 * 60 * 60 * 1000));

        Float costParking = existingVehicle.getCostPerHour();
        Date entryTime = new Date(existingVehicle.getEntryTime().getTime() - (5 * 60 * 60 * 1000));

        long timeDifference = Math.abs(exitTime.getTime() - entryTime.getTime());
        long minutesParked = timeDifference / (1000 * 60);
        long hoursParked = (long) Math.ceil((double) minutesParked / 60);
        Float totalCost = hoursParked * costParking;

        existingVehicle.setExitTime(currentTime);
        existingVehicle.setStatus(VehicleStatus.OUT);
        Vehicle savedVehicle = vehicleRepository.save(existingVehicle);

        return vehicleMapper.toExitResponse(savedVehicle, entryTime, exitTime, totalCost);
    }

    public void deleteVehicle(String id) {
        Vehicle existingVehicle = vehicleRepository.findById(id).orElseThrow(() -> new VehicleNotFoundException());

        vehicleRepository.delete(existingVehicle);
    }

    public List<IndicatorResponse> getFirstTimeParkedVehicles() {
        List<Vehicle> firstTimeVehicles = vehicleRepository.findFirstTimeParkedVehicles(VehicleStatus.IN);

        return firstTimeVehicles.stream()
                .map(vehicle -> new IndicatorResponse(
                        vehicle.getPlateNumber(),
                        vehicle.getModel(),
                        vehicle.getEntryTime(),
                        vehicle.getExitTime(),
                        new IndicatorResponse.ParkingSimpleResponse(vehicle.getParking().getName())
                ))
                .collect(Collectors.toList());
    }
}
