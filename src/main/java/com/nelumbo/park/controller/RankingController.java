package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rankings")
public class RankingController {
    
    private final VehicleService vehicleService;
    
    public RankingController(
            VehicleService vehicleService
    ) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/")
    public List<TopVehicleResponse> getTopVehicles() {
        return vehicleService.getTopVehicles();
    }
    
    @GetMapping("/{id}")
    public List<TopVehicleResponse> getTopVehicleById(@PathVariable String id) {
        return vehicleService.getTopVehicleById(id);
    }

}
