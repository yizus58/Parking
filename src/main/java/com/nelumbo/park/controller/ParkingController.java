package com.nelumbo.park.controller;

import com.nelumbo.park.dto.ParkingRequest;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.service.ParkingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/parkings")
public class ParkingController {

    private final ParkingService parkingService;

    public ParkingController(
            ParkingService parkingService
    ) {
        this.parkingService = parkingService;
    }

    @GetMapping("/")
    public List<Parking> getParkings() {
        return parkingService.getAllParkings();
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Parking createParking(@Validated @RequestBody ParkingRequest parking) {
        return parkingService.createParking(parking);
    }
}
