package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase.VehicleEntryCommand;
import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.application.port.out.ParkingRepositoryPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.LimitParkingFullException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleEntryServiceTest {

    private VehicleRepositoryPort vehicles;
    private ParkingRepositoryPort parkings;
    private CurrentUserPort currentUser;
    private VehicleEntryService service;

    private final User owner = Fixtures.partner("p1");
    private final Parking parking = Fixtures.parking("park-1", owner, 2);
    private final VehicleEntryCommand command = new VehicleEntryCommand("ABC123", "Mazda", "park-1");

    @BeforeEach
    void setUp() {
        vehicles = mock(VehicleRepositoryPort.class);
        parkings = mock(ParkingRepositoryPort.class);
        currentUser = mock(CurrentUserPort.class);
        service = new VehicleEntryService(vehicles, parkings, new CurrentUserResolver(currentUser), Fixtures.CLOCK);
        when(vehicles.findByPlateNumberAndStatus(any(), any())).thenReturn(Optional.empty());
        when(parkings.findByIdForUpdate("park-1")).thenReturn(Optional.of(parking));
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(vehicles.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registersEntry() {
        when(vehicles.countByParkingIdAndStatus("park-1", VehicleStatus.IN)).thenReturn(1L);

        Vehicle saved = service.registerEntry(command);

        assertEquals("ABC123", saved.getPlateNumber());
        assertEquals("Mazda", saved.getModel());
        assertEquals(VehicleStatus.IN, saved.getStatus());
        assertEquals(Fixtures.NOW, saved.getEntryTime());
        assertNull(saved.getExitTime());
        assertEquals(2000f, saved.getCostPerHour());
        assertSame(owner, saved.getAdmin());
        assertSame(parking, saved.getParking());
    }

    @Test
    void plateAlreadyInside() {
        when(vehicles.findByPlateNumberAndStatus("ABC123", VehicleStatus.IN))
                .thenReturn(Optional.of(Fixtures.inside("v1", "ABC123", parking, owner)));

        VehicleAlreadyInParkingException ex = assertThrows(VehicleAlreadyInParkingException.class,
                () -> service.registerEntry(command));
        assertEquals("El vehiculo con placa ABC123 ya está registrado y actualmente en un parking", ex.getMessage());
        verify(vehicles, never()).save(any());
    }

    @Test
    void unknownParking() {
        when(parkings.findByIdForUpdate("park-1")).thenReturn(Optional.empty());

        ParkingNotFoundException ex = assertThrows(ParkingNotFoundException.class, () -> service.registerEntry(command));
        assertEquals("Parking no encontrado con ID: park-1", ex.getMessage());
    }

    @Test
    void locksTheParkingRow() {
        service.registerEntry(command);
        verify(parkings).findByIdForUpdate("park-1");
        verify(parkings, never()).findById(any());
    }

    @Test
    void notOwner() {
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.partner("p2")));
        assertThrows(InsufficientPermissionsException.class, () -> service.registerEntry(command));
        verify(vehicles, never()).save(any());
    }

    @Test
    void parkingFull() {
        when(vehicles.countByParkingIdAndStatus("park-1", VehicleStatus.IN)).thenReturn(2L);

        LimitParkingFullException ex = assertThrows(LimitParkingFullException.class, () -> service.registerEntry(command));
        assertEquals("El limite de vehiculos en parking Parking park-1 ya ha sido alcanzado", ex.getMessage());
        verify(vehicles, never()).save(any());
    }
}
