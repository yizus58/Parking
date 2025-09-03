package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.configuration.security.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.configuration.security.exception.exceptions.NoAssociatedParkingException;
import com.nelumbo.park.configuration.security.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.mapper.ParkingMapper;
import com.nelumbo.park.repository.ParkingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {
    private final ParkingRepository parkingRepository;
    private final ParkingMapper parkingMapper;
    private final SecurityService securityService;

    public ParkingService(
            ParkingMapper parkingMapper,
            ParkingRepository parkingRepository,
            SecurityService securityService
    ) {
        this.parkingMapper = parkingMapper;
        this.parkingRepository = parkingRepository;
        this.securityService = securityService;
    }

    public List<Parking> getAllParkings() {
        User currentUser = securityService.getCurrentUser();

        if (securityService.isAdmin()) {
            return parkingRepository.findAll();
        } else if (securityService.isEmpleado()) {
            List<Parking> userParkings = parkingRepository.findByOwner(currentUser);
            if (userParkings.isEmpty()) {
                throw new NoAssociatedParkingException();
            }
            return userParkings;
        }

        return parkingRepository.findAll();
    }

    public Parking getParkingById(String id) {
        User currentUser = securityService.getCurrentUser();

        if (securityService.isAdmin()) {
            return parkingRepository.findById(id);
        } else if (securityService.isEmpleado()) {
            Parking parking = parkingRepository.findByIdAndOwner(id, currentUser);
            if (parking == null) {
                throw new InsufficientPermissionsException();
            }
            return parking;
        }

        return parkingRepository.findById(id);
    }

    public Parking createParking (ParkingRequest parkingRequest) {
        Parking parking = parkingMapper.toEntity(parkingRequest);
        return parkingRepository.save(parking);
    }

    public Parking updateParking(String id, ParkingUpdateRequest parkingUpdateRequest) {
        Parking existingParking = parkingRepository.findById(id);

        if (existingParking == null) {
            throw new ParkingNotFoundException();
        }

        Parking updatedParking = parkingMapper.toEntity(parkingUpdateRequest);
        updatedParking.setId(id);

        return parkingRepository.save(updatedParking);
    }

    public void deleteParking(String id) {
        Parking existingParking = parkingRepository.findById(id);

        if (existingParking == null) {
            throw new ParkingNotFoundException();
        }

        parkingRepository.delete(existingParking);
    }

}
