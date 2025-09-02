package com.nelumbo.park.service;

import com.nelumbo.park.dto.ParkingRequest;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.mapper.ParkingMapper;
import com.nelumbo.park.repository.ParkingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {
    private final ParkingRepository parkingRepository;
    private final ParkingMapper parkingMapper;

    public ParkingService(
            ParkingMapper parkingMapper,
            ParkingRepository parkingRepository
    ) {
        this.parkingMapper = parkingMapper;
        this.parkingRepository = parkingRepository;
    }

    public List<Parking> getAllParkings() {
        return parkingRepository.findAll();
    }

    public Parking createParking (ParkingRequest parkingRequest) {
        Parking parking = parkingMapper.toEntity(parkingRequest);
        return parkingRepository.save(parking);
    }

}
