package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
import com.nelumbo.park.service.VehicleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners-rankings")
public class PartnersRakingController {
    
    private final VehicleService vehicleService;
    
    public PartnersRakingController(
            VehicleService vehicleService
    ) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WeeklyPartnerStatsResponse getPartnersRanking() {
        return vehicleService.getPartnersRanking();
    }
}
