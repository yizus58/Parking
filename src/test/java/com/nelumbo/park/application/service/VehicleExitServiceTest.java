package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.RegisterVehicleExitUseCase.VehicleExit;
import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.VehicleNotFoundException;
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

class VehicleExitServiceTest {

    private VehicleRepositoryPort vehicles;
    private CurrentUserPort currentUser;
    private VehicleExitService service;

    private final User owner = Fixtures.partner("p1");
    private final Parking parking = Fixtures.parking("park-1", owner, 2);

    @BeforeEach
    void setUp() {
        vehicles = mock(VehicleRepositoryPort.class);
        currentUser = mock(CurrentUserPort.class);
        service = new VehicleExitService(vehicles, new CurrentUserResolver(currentUser), Fixtures.CLOCK);
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(vehicles.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registersExit_uppercasesPlate_andCharges() {
        Vehicle inside = Fixtures.inside("v1", "ABC123", parking, owner); // entered one hour ago
        when(vehicles.findByPlateNumberAndStatus("ABC123", VehicleStatus.IN)).thenReturn(Optional.of(inside));

        VehicleExit exit = service.registerExit("abc123");

        assertEquals(2000f, exit.totalCost());
        assertEquals(VehicleStatus.OUT, exit.vehicle().getStatus());
        assertEquals(Fixtures.NOW, exit.vehicle().getExitTime());
        verify(vehicles).save(inside);
    }

    @Test
    void notInside() {
        when(vehicles.findByPlateNumberAndStatus(any(), any())).thenReturn(Optional.empty());
        assertThrows(VehicleNotFoundException.class, () -> service.registerExit("ABC123"));
    }

    @Test
    void otherPartner_forbidden() {
        when(vehicles.findByPlateNumberAndStatus("ABC123", VehicleStatus.IN))
                .thenReturn(Optional.of(Fixtures.inside("v1", "ABC123", parking, owner)));
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.partner("p2")));
        assertThrows(InsufficientPermissionsException.class, () -> service.registerExit("ABC123"));
        verify(vehicles, never()).save(any());
    }

    @Test
    void missingParking() {
        Vehicle inside = Fixtures.inside("v1", "ABC123", parking, owner);
        inside.setParking(null);
        when(vehicles.findByPlateNumberAndStatus("ABC123", VehicleStatus.IN)).thenReturn(Optional.of(inside));
        assertThrows(ParkingNotFoundException.class, () -> service.registerExit("ABC123"));
    }
}
