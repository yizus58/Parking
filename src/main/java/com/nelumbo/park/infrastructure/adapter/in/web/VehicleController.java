package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ManageVehiclesUseCase;
import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase;
import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase.VehicleEntryCommand;
import com.nelumbo.park.application.port.in.RegisterVehicleExitUseCase;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.request.VehicleCreateRequest;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleCreateResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleExitResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.request.VehicleUpdateRequest;
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

    private final ManageVehiclesUseCase manageVehicles;
    private final RegisterVehicleEntryUseCase registerEntry;
    private final RegisterVehicleExitUseCase registerExit;

    public VehicleController(
            ManageVehiclesUseCase manageVehicles,
            RegisterVehicleEntryUseCase registerEntry,
            RegisterVehicleExitUseCase registerExit) {
        this.manageVehicles = manageVehicles;
        this.registerEntry = registerEntry;
        this.registerExit = registerExit;
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
        return manageVehicles.listVehicles().stream().map(WebMapper::toVehicleResponse).toList();
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
        return WebMapper.toVehicleResponse(manageVehicles.getVehicle(id));
    }

    @Operation(summary = "Registra un vehiculo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehiculo registrado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VehicleCreateResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "403", description = "No tienes permisos para realizar esta accion", content = @Content),
            @ApiResponse(responseCode = "409", description = "El vehiculo ya esta registrado / limite de vehiculos ha sido alcanzado", content = @Content)
    })
    @PostMapping("/")
    @PreAuthorize("hasAuthority('SOCIO')")
    public VehicleCreateResponse createVehicle(@Validated @RequestBody VehicleCreateRequest vehicle) {
        VehicleEntryCommand command = new VehicleEntryCommand(
                vehicle.getPlateNumber(), vehicle.getModelVehicle(), vehicle.getIdParking());
        return WebMapper.toVehicleCreateResponse(registerEntry.registerEntry(command));
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
        return WebMapper.toVehicleExitResponse(registerExit.registerExit(vehicle.getPlateNumber()));
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
        manageVehicles.deleteVehicle(id);
        return ResponseEntity.ok("Vehículo eliminado exitosamente");
    }
}