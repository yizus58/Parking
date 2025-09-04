package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.IndicatorResponse;
import com.nelumbo.park.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/indicators")
public class IndicatorController {

    private final VehicleService vehicleService;

    public IndicatorController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }
    
    @GetMapping("/")
    public List<IndicatorResponse> getFirstTimeParkedVehicles() {
        return vehicleService.getFirstTimeParkedVehicles();
    }
}
