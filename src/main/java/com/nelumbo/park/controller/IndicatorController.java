package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.IndicatorResponse;
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

import java.util.List;

@RestController
@RequestMapping("/indicators")
@Tag(name = "Indicators", description = "Indicator API")
public class IndicatorController {

    private final VehicleService vehicleService;

    public IndicatorController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "Obtiene los vehiculos que han sido parqueados por primera vez")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehiculo encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = IndicatorResponse.class)))
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<IndicatorResponse> getFirstTimeParkedVehicles() {
        return vehicleService.getFirstTimeParkedVehicles();
    }
}
