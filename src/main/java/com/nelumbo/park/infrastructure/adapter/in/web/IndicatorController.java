package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ParkingIndicatorsUseCase;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.IndicatorResponse;
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

    private final ParkingIndicatorsUseCase indicators;

    public IndicatorController(ParkingIndicatorsUseCase indicators) {
        this.indicators = indicators;
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
        return indicators.firstTimeParkedVehicles().stream().map(WebMapper::toIndicator).toList();
    }
}
