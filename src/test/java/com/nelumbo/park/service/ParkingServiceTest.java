package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.response.ParkingWithVehiclesResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.NoAssociatedParkingException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.mapper.ParkingMapper;
import com.nelumbo.park.mapper.ParkingResponseMapper;
import com.nelumbo.park.mapper.ParkingWithVehiclesMapper;
import com.nelumbo.park.repository.ParkingRepository;
import com.nelumbo.park.service.infrastructure.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    @Mock
    private ParkingRepository parkingRepository;
    @Mock
    private ParkingMapper parkingMapper;
    @Mock
    private ParkingResponseMapper parkingResponseMapper;
    @Mock
    private ParkingWithVehiclesMapper parkingWithVehiclesMapper;
    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ParkingService parkingService;

    private User adminUser;
    private User socioUser;
    private Parking parking;
    private ParkingRequest parkingRequest;
    private ParkingUpdateRequest parkingUpdateRequest;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId("admin-id");
        adminUser.setRole("ADMIN");

        socioUser = new User();
        socioUser.setId("socio-id");
        socioUser.setRole("SOCIO");

        parking = new Parking();
        parking.setId("parking-id");
        parking.setName("Test Parking");
        parking.setOwner(socioUser);

        parkingRequest = new ParkingRequest();
        parkingRequest.setName("New Parking");

        parkingUpdateRequest = new ParkingUpdateRequest();
        parkingUpdateRequest.setName("Updated Parking");
    }

    @Test
    void getAllParkings_AsAdmin_ShouldReturnAllParkings() {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(parkingRepository.findAll()).thenReturn(Collections.singletonList(parking));
        when(parkingResponseMapper.toResponseList(any())).thenReturn(Collections.singletonList(new ParkingResponse()));

        List<ParkingResponse> result = parkingService.getAllParkings();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(parkingRepository).findAll();
        verify(parkingRepository, never()).findByOwner(any());
    }

    @Test
    void getAllParkings_AsAdmin_WhenNoParkings_ShouldThrowParkingNotFoundException() {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(parkingRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(ParkingNotFoundException.class, () -> parkingService.getAllParkings());
    }

    @Test
    void getAllParkings_AsSocio_ShouldReturnOwnedParkings() {
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(parkingRepository.findByOwner(socioUser)).thenReturn(Collections.singletonList(parking));
        when(parkingResponseMapper.toResponseListWithoutOwner(any())).thenReturn(Collections.singletonList(new ParkingResponse()));

        List<ParkingResponse> result = parkingService.getAllParkings();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(parkingRepository, never()).findAll();
        verify(parkingRepository).findByOwner(socioUser);
    }

    @Test
    void getAllParkings_AsSocio_WhenNoParkings_ShouldThrowNoAssociatedParkingException() {
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(parkingRepository.findByOwner(socioUser)).thenReturn(Collections.emptyList());

        assertThrows(NoAssociatedParkingException.class, () -> parkingService.getAllParkings());
    }

    @Test
    void getParkingById_AsAdmin_ShouldReturnParking() {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(parkingRepository.findById(parking.getId())).thenReturn(parking);
        when(parkingWithVehiclesMapper.toResponse(parking)).thenReturn(new ParkingWithVehiclesResponse());

        ParkingWithVehiclesResponse result = parkingService.getParkingById(parking.getId());

        assertNotNull(result);
        verify(parkingRepository).findById(parking.getId());
    }

    @Test
    void getParkingById_AsAdmin_WhenNotFound_ShouldThrowParkingNotFoundException() {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(parkingRepository.findById(anyString())).thenReturn(null);

        assertThrows(ParkingNotFoundException.class, () -> parkingService.getParkingById("non-existent-id"));
    }

    @Test
    void getParkingById_AsSocio_WhenIsOwner_ShouldReturnParking() {
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(parkingRepository.findByIdAndOwner(parking.getId(), socioUser)).thenReturn(parking);
        when(parkingWithVehiclesMapper.toResponse(parking)).thenReturn(new ParkingWithVehiclesResponse());

        ParkingWithVehiclesResponse result = parkingService.getParkingById(parking.getId());

        assertNotNull(result);
        verify(parkingRepository).findByIdAndOwner(parking.getId(), socioUser);
    }

    @Test
    void getParkingById_AsSocio_WhenIsNotOwner_ShouldThrowInsufficientPermissionsException() {
        User anotherSocio = new User();
        anotherSocio.setId("another-socio-id");
        anotherSocio.setRole("SOCIO");

        when(securityService.getCurrentUser()).thenReturn(anotherSocio);
        when(parkingRepository.findByIdAndOwner(parking.getId(), anotherSocio)).thenReturn(null);
        when(parkingRepository.findById(parking.getId())).thenReturn(parking);

        assertThrows(InsufficientPermissionsException.class, () -> parkingService.getParkingById(parking.getId()));
    }

    @Test
    void createParking_ShouldSaveAndReturnParking() {
        when(parkingMapper.toEntity(parkingRequest)).thenReturn(parking);
        when(parkingRepository.save(parking)).thenReturn(parking);

        Parking result = parkingService.createParking(parkingRequest);

        assertNotNull(result);
        assertEquals("Test Parking", result.getName());
        verify(parkingMapper).toEntity(parkingRequest);
        verify(parkingRepository).save(parking);
    }

    @Test
    void updateParking_WhenParkingExists_ShouldUpdateAndReturnParking() {
        Parking updatedParking = new Parking();
        updatedParking.setName("Updated Parking");

        when(parkingRepository.findById(parking.getId())).thenReturn(parking);
        when(parkingMapper.toEntity(parkingUpdateRequest)).thenReturn(updatedParking);
        when(parkingRepository.save(any(Parking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Parking result = parkingService.updateParking(parking.getId(), parkingUpdateRequest);

        assertNotNull(result);
        assertEquals(parking.getId(), result.getId());
        assertEquals("Updated Parking", result.getName());
        verify(parkingRepository).findById(parking.getId());
        verify(parkingRepository).save(any(Parking.class));
    }

    @Test
    void updateParking_WhenParkingNotFound_ShouldThrowParkingNotFoundException() {
        when(parkingRepository.findById(anyString())).thenReturn(null);

        assertThrows(ParkingNotFoundException.class, () -> parkingService.updateParking("non-existent-id", parkingUpdateRequest));
        verify(parkingRepository, never()).save(any());
    }

    @Test
    void deleteParking_WhenParkingExists_ShouldDeleteParking() {
        when(parkingRepository.findById(parking.getId())).thenReturn(parking);
        doNothing().when(parkingRepository).delete(parking);

        parkingService.deleteParking(parking.getId());

        verify(parkingRepository).findById(parking.getId());
        verify(parkingRepository).delete(parking);
    }

    @Test
    void deleteParking_WhenParkingNotFound_ShouldThrowParkingNotFoundException() {
        when(parkingRepository.findById(anyString())).thenReturn(null);

        assertThrows(ParkingNotFoundException.class, () -> parkingService.deleteParking("non-existent-id"));
        verify(parkingRepository, never()).delete(any());
    }
}
