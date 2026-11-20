package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ManageParkingsUseCase;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.ParkingCommand;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.VisibleParkings;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.request.ParkingRequest;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.ParkingResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.ParkingWithVehiclesResponse;
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
@RequestMapping("/parkings")
@Tag(name = "Parkings", description = "Parking API")
public class ParkingController {

    private final ManageParkingsUseCase manageParkings;

    public ParkingController(ManageParkingsUseCase manageParkings) {
        this.manageParkings = manageParkings;
    }

    @Operation(summary = "Obtiene todos los parkings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parkings encontrados",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ParkingResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes parkings asociados", content = @Content),
            @ApiResponse(responseCode = "404", description = "El parking no existe", content = @Content)
    })
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public List<ParkingResponse> getParkings() {
        VisibleParkings visible = manageParkings.listParkings();
        return visible.parkings().stream()
                .map(visible.ownerVisible() ? WebMapper::toParkingResponse : WebMapper::toParkingResponseWithoutOwner)
                .toList();
    }

    @Operation(summary = "Obtiene un parking por su id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ParkingWithVehiclesResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para acceder al parking", content = @Content),
            @ApiResponse(responseCode = "404", description = "El parking no existe", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('SOCIO')")
    public ParkingWithVehiclesResponse getParkingById(@PathVariable String id) {
        return WebMapper.toParkingWithVehicles(manageParkings.getParking(id));
    }

    @Operation(summary = "Crea un parking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parking creado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ParkingResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta acción", content = @Content)
    })
    @PostMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ParkingResponse createParking(@Validated @RequestBody ParkingRequest parking) {
        ParkingCommand command = new ParkingCommand(parking.getName(), parking.getAddress(),
                parking.getCapacity(), parking.getCostPerHour(), parking.getIdOwner());
        return WebMapper.toCreatedParkingResponse(manageParkings.createParking(command));
    }

    @Operation(summary = "Actualiza un parking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parking actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ParkingResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta acción", content = @Content),
            @ApiResponse(responseCode = "404", description = "El parking no existe", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ParkingResponse updateParking(@PathVariable String id, @Validated @RequestBody ParkingUpdateRequest parking) {
        ParkingCommand command = new ParkingCommand(parking.getName(), parking.getAddress(),
                parking.getCapacity(), parking.getCostPerHour(), parking.getIdOwner());
        return WebMapper.toParkingResponse(manageParkings.updateParking(id, command));
    }

    @Operation(summary = "Elimina un parking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parking eliminado exitosamente",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResponseEntity.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta acción", content = @Content),
            @ApiResponse(responseCode = "404", description = "El parking no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteParking(@PathVariable String id) {
        manageParkings.deleteParking(id);
        return ResponseEntity.ok("Parking eliminado exitosamente");
    }

}
