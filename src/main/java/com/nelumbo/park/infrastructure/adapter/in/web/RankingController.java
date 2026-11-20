package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ParkingIndicatorsUseCase;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.TopVehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rankings")
@Tag(name = "Rankings", description = "Ranking API")
public class RankingController {
    
    private final ParkingIndicatorsUseCase indicators;
    
    public RankingController(ParkingIndicatorsUseCase indicators) {
        this.indicators = indicators;
    }

    @Operation(summary = "Obtiene el top 10 vehículos que más veces se han registrado en los diferentes \n" +
            "parqueaderos y cuantas veces han sido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TopVehicleResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<TopVehicleResponse> getTopVehicles() {
        return indicators.topVehicles().stream().map(WebMapper::toTopVehicle).toList();
    }
    
    @Operation(summary = "Obtiene el top 10 vehículos que más veces se han registrado en un \n" +
            "parqueadero especifico y cuantas veces han sido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TopVehicleResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "404", description = "El parqueadero no existe", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<TopVehicleResponse> getTopVehicleById(@PathVariable String id) {
        return indicators.topVehiclesByParking(id).stream().map(WebMapper::toTopVehicle).toList();
    }

}
