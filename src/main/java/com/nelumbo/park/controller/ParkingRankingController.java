package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parking-rankings")
@Tag(name = "Parking Rankings", description = "Parking Ranking API")
public class ParkingRankingController {

    private final VehicleService vehicleService;

    public ParkingRankingController(
            VehicleService vehicleService
    ) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "Obtiene los top 3 de parqueaderos con mayor ganancia")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WeeklyParkingStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WeeklyParkingStatsResponse getParkingRanking() {
        return vehicleService.getParkingRanking();
    }
}
