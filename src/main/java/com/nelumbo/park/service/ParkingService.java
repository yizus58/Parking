package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.response.ParkingWithVehiclesResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.NoAssociatedParkingException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.mapper.ParkingMapper;
import com.nelumbo.park.mapper.ParkingResponseMapper;
import com.nelumbo.park.mapper.ParkingWithVehiclesMapper;
import com.nelumbo.park.repository.ParkingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {
    private final ParkingRepository parkingRepository;
    private final ParkingMapper parkingMapper;
    private final ParkingResponseMapper parkingResponseMapper;
    private final ParkingWithVehiclesMapper parkingWithVehiclesMapper;
    private final SecurityService securityService;

    public ParkingService(
            ParkingMapper parkingMapper,
            ParkingRepository parkingRepository,
            ParkingResponseMapper parkingResponseMapper,
            ParkingWithVehiclesMapper parkingWithVehiclesMapper,
            SecurityService securityService
    ) {
        this.parkingMapper = parkingMapper;
        this.parkingRepository = parkingRepository;
        this.parkingResponseMapper = parkingResponseMapper;
        this.parkingWithVehiclesMapper = parkingWithVehiclesMapper;
        this.securityService = securityService;
    }

    public List<ParkingResponse> getAllParkings() {
        User currentUser = securityService.getCurrentUser();

        if ("SOCIO".equals(currentUser.getRole())) {
            List<Parking> userParkings = parkingRepository.findByOwner(currentUser);
            if (userParkings.isEmpty()) {
                throw new NoAssociatedParkingException();
            }
            return parkingResponseMapper.toResponseListWithoutOwner(userParkings);
        }

        List<Parking> parkings = parkingRepository.findAll();
        if (parkings.isEmpty()) {
            throw new ParkingNotFoundException();
        }
        return parkingResponseMapper.toResponseList(parkings);
    }

    public ParkingWithVehiclesResponse getParkingById(String id) {
        User currentUser = securityService.getCurrentUser();

        if ("SOCIO".equals(currentUser.getRole())) {
            Parking parking = parkingRepository.findByIdAndOwner(id, currentUser);
            if (parking == null) {
                Parking newParking = parkingRepository.findById(id);
                if (newParking.getOwner() != currentUser) {
                    throw new InsufficientPermissionsException();
                }

                throw new NoAssociatedParkingException();
            }
            return parkingWithVehiclesMapper.toResponse(parking);
        }

        Parking parking = parkingRepository.findById(id);
        if (parking == null) {
            throw new ParkingNotFoundException();
        }
        return parkingWithVehiclesMapper.toResponse(parking);
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
