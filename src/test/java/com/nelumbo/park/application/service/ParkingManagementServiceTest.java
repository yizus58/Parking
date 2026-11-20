package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.ManageParkingsUseCase.ParkingCommand;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.ParkingDetails;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.VisibleParkings;
import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.application.port.out.ParkingRepositoryPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.NoAssociatedParkingException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.UserNotFoundException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParkingManagementServiceTest {

    private ParkingRepositoryPort parkings;
    private VehicleRepositoryPort vehicles;
    private UserRepositoryPort users;
    private CurrentUserPort currentUser;
    private ParkingManagementService service;

    private final User admin = Fixtures.admin();
    private final User owner = Fixtures.partner("p1");
    private final Parking parking = Fixtures.parking("park-1", owner, 10);

    @BeforeEach
    void setUp() {
        parkings = mock(ParkingRepositoryPort.class);
        vehicles = mock(VehicleRepositoryPort.class);
        users = mock(UserRepositoryPort.class);
        currentUser = mock(CurrentUserPort.class);
        service = new ParkingManagementService(parkings, vehicles, users, new CurrentUserResolver(currentUser));
    }

    @Test
    void list_asAdmin_allWithOwner() {
        when(currentUser.currentUser()).thenReturn(Optional.of(admin));
        when(parkings.findAll()).thenReturn(List.of(parking));

        VisibleParkings result = service.listParkings();

        assertTrue(result.ownerVisible());
        assertEquals(List.of(parking), result.parkings());
    }

    @Test
    void list_asAdmin_emptyThrows() {
        when(currentUser.currentUser()).thenReturn(Optional.of(admin));
        when(parkings.findAll()).thenReturn(List.of());
        assertThrows(ParkingNotFoundException.class, () -> service.listParkings());
    }

    @Test
    void list_asPartner_ownWithoutOwner() {
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(parkings.findByOwnerId("p1")).thenReturn(List.of(parking));

        VisibleParkings result = service.listParkings();

        assertFalse(result.ownerVisible());
        assertEquals(1, result.parkings().size());
    }

    @Test
    void list_asPartner_noneThrows() {
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(parkings.findByOwnerId("p1")).thenReturn(List.of());
        assertThrows(NoAssociatedParkingException.class, () -> service.listParkings());
    }

    @Test
    void get_asOwner_includesVehicles() {
        when(currentUser.currentUser()).thenReturn(Optional.of(owner));
        when(parkings.findById("park-1")).thenReturn(Optional.of(parking));
        when(vehicles.findByParkingId("park-1")).thenReturn(List.of(Fixtures.inside("v1", "ABC123", parking, owner)));

        ParkingDetails details = service.getParking("park-1");

        assertSame(parking, details.parking());
        assertEquals(1, details.vehicles().size());
    }

    @Test
    void get_asOtherPartner_forbidden() {
        when(currentUser.currentUser()).thenReturn(Optional.of(Fixtures.partner("p2")));
        when(parkings.findById("park-1")).thenReturn(Optional.of(parking));
        assertThrows(InsufficientPermissionsException.class, () -> service.getParking("park-1"));
    }

    @Test
    void get_missing_notFound() {
        when(currentUser.currentUser()).thenReturn(Optional.of(admin));
        when(parkings.findById("nope")).thenReturn(Optional.empty());
        assertThrows(ParkingNotFoundException.class, () -> service.getParking("nope"));
    }

    @Test
    void create_resolvesOwner() {
        when(users.findById("p1")).thenReturn(Optional.of(owner));
        when(parkings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Parking created = service.createParking(new ParkingCommand("Parking Norte", "Calle 1 # 2-3", 5, 1500f, "p1"));

        assertNull(created.getId());
        assertSame(owner, created.getOwner());
        assertEquals(5, created.getCapacity());
    }

    @Test
    void create_unknownOwner() {
        when(users.findById("x")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> service.createParking(new ParkingCommand("Parking Norte", "Calle 1 # 2-3", 5, 1500f, "x")));
        verify(parkings, never()).save(any());
    }

    @Test
    void update_keepsId() {
        when(parkings.findById("park-1")).thenReturn(Optional.of(parking));
        when(users.findById("p1")).thenReturn(Optional.of(owner));
        when(parkings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Parking updated = service.updateParking("park-1", new ParkingCommand("Nuevo nombre", "Calle 1 # 2-3", 7, 1000f, "p1"));

        assertEquals("park-1", updated.getId());
        assertEquals("Nuevo nombre", updated.getName());
    }

    @Test
    void update_missing() {
        when(parkings.findById("nope")).thenReturn(Optional.empty());
        assertThrows(ParkingNotFoundException.class,
                () -> service.updateParking("nope", new ParkingCommand("n", "a", 1, 1f, "p1")));
    }

    @Test
    void delete_existingAndMissing() {
        when(parkings.findById("park-1")).thenReturn(Optional.of(parking));
        service.deleteParking("park-1");
        verify(parkings).deleteById("park-1");

        when(parkings.findById("nope")).thenReturn(Optional.empty());
        assertThrows(ParkingNotFoundException.class, () -> service.deleteParking("nope"));
    }
}
