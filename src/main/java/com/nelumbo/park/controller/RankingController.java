package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
    
    private final VehicleService vehicleService;
    
    public RankingController(
            VehicleService vehicleService
    ) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "Obtiene el top 10 vehículos que más veces se han registrado en los diferentes \n" +
            "parqueaderos y cuantas veces han sido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<TopVehicleResponse> getTopVehicles() {
        return vehicleService.getTopVehicles();
    }
    
    @Operation(summary = "Obtiene el top 10 vehículos que más veces se han registrado en un \n" +
            "parqueadero especifico y cuantas veces han sido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<TopVehicleResponse> getTopVehicleById(@PathVariable String id) {
        return vehicleService.getTopVehicleById(id);
    }

}
