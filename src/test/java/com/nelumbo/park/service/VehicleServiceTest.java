package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.dto.response.IndicatorResponse;
import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.dto.response.VehicleCreateResponse;
import com.nelumbo.park.dto.response.VehicleExitResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.mapper.VehicleMapper;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.repository.VehicleRepository;
import com.nelumbo.park.service.infrastructure.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.Optional;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleMapper vehicleMapper;
    @Mock
    private SecurityService securityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParkingStatsService parkingStatsService;

    @InjectMocks
    private VehicleService vehicleService;

    private User adminUser;
    private User socioUser;
    private Vehicle vehicle;
    private Parking parking;
    private VehicleCreateRequest createRequest;
    private VehicleUpdateRequest updateRequest;

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
        parking.setName("Central Park");
        parking.setCostPerHour(10.0f);

        vehicle = new Vehicle();
        vehicle.setId("vehicle-id");
        vehicle.setPlateNumber("ABC-123");
        vehicle.setModel("Toyota");
        vehicle.setStatus(VehicleStatus.IN);
        vehicle.setAdmin(socioUser);
        vehicle.setParking(parking);
        vehicle.setEntryTime(new Date(System.currentTimeMillis() - 3600 * 1000));
        vehicle.setCostPerHour(10.0f);

        createRequest = new VehicleCreateRequest();
        createRequest.setPlateNumber("NEW-456");
        createRequest.setIdParking(parking.getId());

        updateRequest = new VehicleUpdateRequest();
        updateRequest.setPlateNumber("ABC-123");
    }

    @Test
    void createVehicle_WhenNotAlreadyParked_ShouldCreateVehicle() {
        Vehicle vehicleFromMapper = new Vehicle();
        vehicleFromMapper.setPlateNumber(createRequest.getPlateNumber());
        vehicleFromMapper.setParking(parking);

        when(vehicleRepository.findByPlateNumberAndStatus(createRequest.getPlateNumber(), VehicleStatus.IN)).thenReturn(Optional.empty());
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(userRepository.findById(socioUser.getId())).thenReturn(Optional.of(socioUser));
        when(vehicleMapper.toEntity(createRequest)).thenReturn(vehicleFromMapper);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(vehicleMapper.toSimpleResponse(vehicle)).thenReturn(new VehicleCreateResponse());

        VehicleCreateResponse response = vehicleService.createVehicle(createRequest);

        assertNotNull(response);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_WhenAlreadyParked_ShouldThrowVehicleAlreadyInParkingException() {
        Vehicle vehicleFromMapper = new Vehicle();
        vehicleFromMapper.setPlateNumber(createRequest.getPlateNumber());

        when(vehicleMapper.toEntity(createRequest)).thenReturn(vehicleFromMapper);
        when(vehicleRepository.findByPlateNumberAndStatus(createRequest.getPlateNumber(), VehicleStatus.IN)).thenReturn(Optional.of(vehicle));

        assertThrows(VehicleAlreadyInParkingException.class, () -> vehicleService.createVehicle(createRequest));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void exitVehicle_WhenVehicleExistsAndIsOwner_ShouldProcessExit() {
        when(vehicleRepository.findByPlateNumberAndStatus(anyString(), eq(VehicleStatus.IN))).thenReturn(Optional.of(vehicle));
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(securityService.isSocio()).thenReturn(true);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(vehicleMapper.toExitResponse(any(), any(), any(), anyFloat())).thenReturn(new VehicleExitResponse());

        VehicleExitResponse response = vehicleService.exitVehicle(updateRequest);

        assertNotNull(response);
        verify(vehicleRepository).save(argThat(v -> v.getStatus() == VehicleStatus.OUT && v.getExitTime() != null));
    }

    @Test
    void exitVehicle_WhenVehicleIsAlreadyOut_ShouldThrowVehicleNotFoundException() {
        when(vehicleRepository.findByPlateNumberAndStatus(anyString(), eq(VehicleStatus.IN))).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.exitVehicle(updateRequest));
    }

    @Test
    void exitVehicle_AsSocio_WhenNotOwner_ShouldThrowInsufficientPermissionsException() {
        User anotherSocio = new User();
        anotherSocio.setId("another-id");

        when(vehicleRepository.findByPlateNumberAndStatus(anyString(), eq(VehicleStatus.IN))).thenReturn(Optional.of(vehicle));
        when(securityService.getCurrentUser()).thenReturn(anotherSocio);
        when(securityService.isSocio()).thenReturn(true);

        assertThrows(InsufficientPermissionsException.class, () -> vehicleService.exitVehicle(updateRequest));
    }

    @Test
    void getAllVehicles_AsAdmin_ShouldReturnAll() {
        when(securityService.isAdmin()).thenReturn(true);
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(vehicleRepository.findAll()).thenReturn(Collections.singletonList(vehicle));

        List<Vehicle> result = vehicleService.getAllVehicles();

        assertFalse(result.isEmpty());
        verify(vehicleRepository).findAll();
    }

    @Test
    void getAllVehicles_AsSocio_ShouldReturnOwned() {
        when(securityService.isSocio()).thenReturn(true);
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(vehicleRepository.findByAdmin(socioUser)).thenReturn(Collections.singletonList(vehicle));

        List<Vehicle> result = vehicleService.getAllVehicles();

        assertFalse(result.isEmpty());
        verify(vehicleRepository).findByAdmin(socioUser);
    }

    @Test
    void getVehicleById_AsSocioAndOwner_ShouldReturnVehicle() {
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(securityService.getCurrentUser()).thenReturn(socioUser);
        when(securityService.isSocio()).thenReturn(true);

        Vehicle result = vehicleService.getVehicleById(vehicle.getId());

        assertNotNull(result);
        assertEquals(vehicle.getId(), result.getId());
    }

    @Test
    void getVehicleById_AsSocio_WhenNotOwner_ShouldThrowInsufficientPermissionsException() {
        User anotherSocio = new User();
        anotherSocio.setId("another-id");

        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(securityService.getCurrentUser()).thenReturn(anotherSocio);
        when(securityService.isSocio()).thenReturn(true);

        assertThrows(InsufficientPermissionsException.class, () -> vehicleService.getVehicleById(vehicle.getId()));
    }

    @Test
    void getFirstTimeParkedVehicles_ShouldReturnMappedResponse() {
        when(vehicleRepository.findFirstTimeParkedVehicles(VehicleStatus.IN)).thenReturn(Collections.singletonList(vehicle));

        List<IndicatorResponse> result = vehicleService.getFirstTimeParkedVehicles();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(vehicle.getPlateNumber(), result.get(0).getPlateNumber());
        assertEquals(vehicle.getParking().getName(), result.get(0).getParking().getName());
    }

    @Test
    void getTopVehicles_ShouldReturnMappedResponse() {
        Object[] topVehicleData = {"ABC-123", 5L};
        when(vehicleRepository.findTopVehiclesByVisits(any(Pageable.class))).thenReturn(Collections.singletonList(topVehicleData));

        List<TopVehicleResponse> result = vehicleService.getTopVehicles();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("ABC-123", result.get(0).getPlateNumber());
        assertEquals(5L, result.get(0).getTotalVisits());
    }

    @Test
    void getTopVehicleById_ShouldReturnMappedResponse() {
        Object[] topVehicleData = {"ABC-123", 5L};
        when(vehicleRepository.findTopVehicleById(anyString(), any(Pageable.class))).thenReturn(Collections.singletonList(topVehicleData));

        List<TopVehicleResponse> result = vehicleService.getTopVehicleById("parking-id");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("ABC-123", result.get(0).getPlateNumber());
        assertEquals(5L, result.get(0).getTotalVisits());
    }

    @Test
    void getTopVehicleById_WhenNoVehicles_ShouldThrowParkingNotFoundException() {
        when(vehicleRepository.findTopVehicleById(anyString(), any(Pageable.class))).thenReturn(Collections.emptyList());

        assertThrows(ParkingNotFoundException.class, () -> vehicleService.getTopVehicleById("parking-id"));
    }

    @Test
    void getPartnersRanking_ShouldCallParkingStatsService() {
        vehicleService.getPartnersRanking();
        verify(parkingStatsService, times(1)).getPartnersRanking();
    }

    @Test
    void getParkingRanking_ShouldCallParkingStatsService() {
        vehicleService.getParkingRanking();
        verify(parkingStatsService, times(1)).getParkingRanking();
    }

    @Test
    void deleteVehicle_WhenFound_ShouldDelete() {
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        doNothing().when(vehicleRepository).delete(vehicle);

        vehicleService.deleteVehicle(vehicle.getId());

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteVehicle_WhenNotFound_ShouldThrowVehicleNotFoundException() {
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.deleteVehicle("non-existent-id"));
        verify(vehicleRepository, never()).delete(any());
    }
}
