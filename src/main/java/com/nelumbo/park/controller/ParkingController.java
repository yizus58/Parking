package com.nelumbo.park.controller;

import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.mapper.ParkingResponseMapper;
import com.nelumbo.park.service.ParkingService;
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
public class ParkingController {

    private final ParkingService parkingService;
    private final ParkingResponseMapper parkingResponseMapper;

    public ParkingController(
            ParkingService parkingService,
            ParkingResponseMapper parkingResponseMapper
    ) {
        this.parkingService = parkingService;
        this.parkingResponseMapper = parkingResponseMapper;
    }

    @GetMapping("/")
    public List<ParkingResponse> getParkings() {
        List<Parking> parkings = parkingService.getAllParkings();
        return parkingResponseMapper.toResponseList(parkings);
    }

    @GetMapping("/{id}")
    public ParkingResponse getParkingById(@PathVariable String id) {
        Parking parking = parkingService.getParkingById(id);
        return parkingResponseMapper.toResponse(parking);
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ParkingResponse createParking(@Validated @RequestBody ParkingRequest parking) {
        Parking createdParking = parkingService.createParking(parking);
        return parkingResponseMapper.toResponse(createdParking);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ParkingResponse updateParking(@PathVariable String id, @Validated @RequestBody ParkingUpdateRequest parking) {
        Parking updatedParking = parkingService.updateParking(id, parking);
        return parkingResponseMapper.toResponse(updatedParking);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteParking(@PathVariable String id) {
        parkingService.deleteParking(id);
        return ResponseEntity.ok("Parking eliminado exitosamente");
    }

}
