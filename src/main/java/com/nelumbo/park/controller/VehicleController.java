package com.nelumbo.park.controller;

import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.response.VehicleCreateResponse;
import com.nelumbo.park.dto.response.VehicleExitResponse;
import com.nelumbo.park.dto.response.VehicleResponse;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.mapper.VehicleResponseMapper;
import com.nelumbo.park.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@Tag(name = "Vehicles", description = "Vehicle API")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleResponseMapper vehicleResponseMapper;

    public VehicleController(
            VehicleService vehicleService,
            VehicleResponseMapper vehicleResponseMapper) {
        this.vehicleService = vehicleService;
        this.vehicleResponseMapper = vehicleResponseMapper;
    }

    @Operation(summary = "Obtiene todos los vehiculos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehiculos encontrados",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<VehicleResponse> getVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return vehicleResponseMapper.toResponseList(vehicles);
    }

    @Operation(summary = "Obtiene un vehiculo por su id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehiculo encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "404", description = "El vehiculo no existe", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SOCIO')")
    public VehicleResponse getVehicleById(@PathVariable String id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return vehicleResponseMapper.toResponse(vehicle);
    }

    @Operation(summary = "Registra un vehiculo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehiculo registrado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehicleCreateResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "409", description = "El vehiculo ya esta registrado", content = @Content)
    })
    @PostMapping("/")
    @PreAuthorize("hasAuthority('SOCIO')")
    public VehicleCreateResponse createVehicle(@Validated @RequestBody VehicleCreateRequest vehicle) {
        return vehicleService.createVehicle(vehicle);
    }

    @Operation(summary = "Actualiza un vehiculo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El estado del vehiculo ha sido actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehicleExitResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "404", description = "El vehiculo o el parqueadero no existe", content = @Content),
            @ApiResponse(responseCode = "409", description = "El vehículo ya tiene salida registrada", content = @Content)
    })
    @PutMapping("/")
    @PreAuthorize("hasAuthority('SOCIO')")
    public VehicleExitResponse exitVehicle(@Validated @RequestBody VehicleUpdateRequest vehicle) {
        return vehicleService.exitVehicle(vehicle);
    }

    @Operation(summary = "Elimina un vehiculo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehiculo eliminado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResponseEntity.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "404", description = "El vehiculo no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SOCIO')")
    public ResponseEntity<String> deleteVehicle(@PathVariable String id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok("Vehículo eliminado exitosamente");
    }
}