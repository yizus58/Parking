package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.VehicleNotFoundException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleManagementServiceTest {

    private VehicleRepositoryPort vehicles;
    private CurrentUserPort currentUser;
    private VehicleManagementService service;

    private final User owner = Fixtures.partner("p1");
    private final Parking parking = Fixtures.parking("park-1", owner, 10);
    private final Vehicle vehicle = Fixtures.inside("v1", "ABC123", parking, owner);

    @BeforeEach
    void setUp() {
        vehicles = mock(VehicleRepositoryPort.class);
        currentUser = mock(CurrentUserPort.class);
        service = new VehicleManagementService(vehicles, new CurrentUserResolver(currentUser));
    }

    @Test
    void list_adminSeesAll() {
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.admin()));
        when(vehicles.findAll()).thenReturn(List.of(vehicle));
        assertEquals(1, service.listVehicles().size());
        verify(vehicles, never()).findByAdminId(any());
    }

    @Test
    void list_partnerSeesOwn() {
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(vehicles.findByAdminId("p1")).thenReturn(List.of(vehicle));
        assertEquals(1, service.listVehicles().size());
        verify(vehicles, never()).findAll();
    }

    @Test
    void list_otherRoleSeesAll() {
        when(currentUser.currentUser()).thenReturn(Optional.of(new User("x", "x", "h", "x@example.com", "OTHER")));
        when(vehicles.findAll()).thenReturn(List.of());
        assertEquals(0, service.listVehicles().size());
        verify(vehicles).findAll();
    }

    @Test
    void get_partnerOwnVehicle() {
        when(vehicles.findById("v1")).thenReturn(Optional.of(vehicle));
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        assertSame(vehicle, service.getVehicle("v1"));
    }

    @Test
    void get_partnerOtherVehicle_forbidden() {
        when(vehicles.findById("v1")).thenReturn(Optional.of(vehicle));
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.partner("p2")));
        assertThrows(InsufficientPermissionsException.class, () -> service.getVehicle("v1"));
    }

    @Test
    void get_missing() {
        when(vehicles.findById("nope")).thenReturn(Optional.empty());
        assertThrows(VehicleNotFoundException.class, () -> service.getVehicle("nope"));
    }

    @Test
    void delete_byParkingOwner() {
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(vehicles.findById("v1")).thenReturn(Optional.of(vehicle));
        service.deleteVehicle("v1");
        verify(vehicles).deleteById("v1");
    }

    @Test
    void delete_byAdmin() {
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.admin()));
        when(vehicles.findById("v1")).thenReturn(Optional.of(vehicle));
        service.deleteVehicle("v1");
        verify(vehicles).deleteById("v1");
    }

    @Test
    void delete_missing_isNotFound() {
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(vehicles.findById("nope")).thenReturn(Optional.empty());
        assertThrows(VehicleNotFoundException.class, () -> service.deleteVehicle("nope"));
        verify(vehicles, org.mockito.Mockito.never()).deleteById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void delete_byAnotherPartner_isForbidden() {
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.partner("p2")));
        when(vehicles.findById("v1")).thenReturn(Optional.of(vehicle));
        assertThrows(InsufficientPermissionsException.class, () -> service.deleteVehicle("v1"));
        verify(vehicles, org.mockito.Mockito.never()).deleteById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void delete_vehicleWithoutParking_isForbiddenForPartners() {
        Vehicle orphan = new Vehicle("v2", "XYZ987", "Mazda", Fixtures.NOW, null, 1000f,
                com.nelumbo.park.domain.model.VehicleStatus.IN, null, owner);
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(vehicles.findById("v2")).thenReturn(Optional.of(orphan));
        assertThrows(InsufficientPermissionsException.class, () -> service.deleteVehicle("v2"));
    }

    @Test
    void delete_withoutAuthenticatedUser_isForbidden() {
        when(currentUser.currentUser()).thenReturn(Optional.empty());
        when(vehicles.findById("v1")).thenReturn(Optional.of(vehicle));
        assertThrows(InsufficientPermissionsException.class, () -> service.deleteVehicle("v1"));
    }

    private static String any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
