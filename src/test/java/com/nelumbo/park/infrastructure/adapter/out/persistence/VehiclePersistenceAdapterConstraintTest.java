package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.ParkingJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.VehicleJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehiclePersistenceAdapterConstraintTest {

    private final VehicleJpaRepository vehicleJpa = mock(VehicleJpaRepository.class);
    private final VehiclePersistenceAdapter adapter = new VehiclePersistenceAdapter(
            vehicleJpa, mock(ParkingJpaRepository.class), mock(UserJpaRepository.class));
    private final User owner = new User("u1", "socio", "hash", "s@example.com", "SOCIO");
    private final Vehicle vehicle = Vehicle.enter("ABC123", "Mazda",
            new Parking("p1", "Centro", "Calle 1", 5, 1000f, owner), owner, Instant.parse("2026-01-01T10:00:00Z"));

    private static DataIntegrityViolationException violation(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("duplicate key", new SQLException("duplicate key", "23505"), constraint));
    }

    @Test
    void activePlateIndexViolation_becomesTheDomainConflict() {
        when(vehicleJpa.saveAndFlush(any())).thenThrow(violation("ux_vehicles_plate_inside"));

        VehicleAlreadyInParkingException e = assertThrows(VehicleAlreadyInParkingException.class, () -> adapter.save(vehicle));
        assertEquals("El vehiculo con placa ABC123 ya está registrado y actualmente en un parking", e.getMessage());
    }

    @Test
    void otherViolations_arePropagatedUnchanged() {
        DataIntegrityViolationException other = violation("fk_something");
        when(vehicleJpa.saveAndFlush(any())).thenThrow(other);

        assertSame(other, assertThrows(DataIntegrityViolationException.class, () -> adapter.save(vehicle)));
    }

    @Test
    void violates_matchesByConstraintNameOrMessage() {
        assertTrue(VehiclePersistenceAdapter.violates(violation("UX_VEHICLES_PLATE_INSIDE"), "ux_vehicles_plate_inside"));
        assertTrue(VehiclePersistenceAdapter.violates(new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"ux_vehicles_plate_inside\""), "ux_vehicles_plate_inside"));
        assertFalse(VehiclePersistenceAdapter.violates(violation(null), "ux_vehicles_plate_inside"));
    }
}
