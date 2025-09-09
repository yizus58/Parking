package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
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
@RequestMapping("/partners-rankings")
@Tag(name = "Partners Rankings", description = "Partners Ranking API")
public class PartnersRankingController {
    
    private final VehicleService vehicleService;
    
    public PartnersRankingController(
            VehicleService vehicleService
    ) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "Obtiene los top 3 de partners con mayor ganancia")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WeeklyPartnerStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WeeklyPartnerStatsResponse getPartnersRanking() {
        return vehicleService.getPartnersRanking();
    }
}
