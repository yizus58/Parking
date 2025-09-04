package com.nelumbo.park.controller;

import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.response.VehicleResponse;
import com.nelumbo.park.dto.response.VehicleSimpleResponse;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.mapper.VehicleResponseMapper;
import com.nelumbo.park.service.ParkingService;
import com.nelumbo.park.service.VehicleService;
import com.nelumbo.park.service.SecurityService;
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
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleResponseMapper vehicleResponseMapper;

    public VehicleController(
            VehicleService vehicleService,
            VehicleResponseMapper vehicleResponseMapper) {
        this.vehicleService = vehicleService;
        this.vehicleResponseMapper = vehicleResponseMapper;
    }

    @GetMapping("/")
    public List<VehicleResponse> getVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return vehicleResponseMapper.toResponseList(vehicles);
    }

    @GetMapping("/{id}")
    public VehicleResponse getVehicleById(@PathVariable String id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return vehicleResponseMapper.toResponse(vehicle);
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('EMPLEADO')")
    public VehicleSimpleResponse createVehicle(@Validated @RequestBody VehicleCreateRequest vehicle) {
        return vehicleService.createVehicle(vehicle);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADO')")
    public VehicleResponse updateVehicle(@PathVariable String id, @Validated @RequestBody VehicleUpdateRequest vehicle) {
        Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle);
        return vehicleResponseMapper.toResponse(updatedVehicle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADO')")
    public ResponseEntity<String> deleteVehicle(@PathVariable String id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok("Vehículo eliminado exitosamente");
    }
}