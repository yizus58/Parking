package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.service.VehicleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parking-rankings")
public class ParkingRakingController {

    private final VehicleService vehicleService;

    public ParkingRakingController(
            VehicleService vehicleService
    ) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WeeklyParkingStatsResponse getParkingRaking() {
        return vehicleService.getParkingRaking();
    }
}
