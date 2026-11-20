package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.domain.model.stats.TopVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({UserPersistenceAdapter.class, ParkingPersistenceAdapter.class,
        VehiclePersistenceAdapter.class, LoginLogPersistenceAdapter.class})
class PersistenceAdaptersTest {

    private static final Instant T0 = Instant.parse("2026-01-10T15:00:00Z");

    @Autowired
    private UserPersistenceAdapter users;
    @Autowired
    private ParkingPersistenceAdapter parkings;
    @Autowired
    private VehiclePersistenceAdapter vehicles;
    @Autowired
    private TestEntityManager entityManager;

    private User partner;
    private Parking parking;

    @BeforeEach
    void setUp() {
        partner = users.save(User.newPartner("socio1", "socio1@example.com", "hash"));
        parking = parkings.save(new Parking(null, "Parking Centro", "Calle 10 # 20-30", 2, 1000f, partner));
    }

    @Test
    void user_roundTrip() {
        assertNotNull(partner.getId());
        assertEquals("SOCIO", users.findByEmail("socio1@example.com").orElseThrow().getRole());
        assertEquals(partner.getId(), users.findByUsername("socio1").orElseThrow().getId());
        assertTrue(users.findByEmail("nobody@example.com").isEmpty());
    }

    @Test
    void parking_roundTripAndUpdateKeepsId() {
        Parking loaded = parkings.findById(parking.getId()).orElseThrow();
        assertEquals("Parking Centro", loaded.getName());
        assertEquals(partner.getId(), loaded.getOwner().getId());

        loaded.setName("Parking Norte");
        Parking updated = parkings.save(loaded);

        assertEquals(parking.getId(), updated.getId());
        assertEquals(1, parkings.findByOwnerId(partner.getId()).size());
        assertEquals("Parking Norte", parkings.findById(parking.getId()).orElseThrow().getName());
    }

    @Test
    void vehicle_entryExitAndQueries() {
        Vehicle saved = vehicles.save(Vehicle.enter("ABC123", "Mazda", parking, partner, T0));
        assertNotNull(saved.getId());
        assertEquals(1, vehicles.countByParkingIdAndStatus(parking.getId(), VehicleStatus.IN));
        assertTrue(vehicles.findByPlateNumberAndStatus("ABC123", VehicleStatus.IN).isPresent());

        Vehicle inside = vehicles.findById(saved.getId()).orElseThrow();
        inside.exit(T0.plusSeconds(3600));
        vehicles.save(inside);

        assertEquals(0, vehicles.countByParkingIdAndStatus(parking.getId(), VehicleStatus.IN));
        Vehicle out = vehicles.findById(saved.getId()).orElseThrow();
        assertEquals(VehicleStatus.OUT, out.getStatus());
        assertEquals(T0.plusSeconds(3600), out.getExitTime());
        assertEquals(1, vehicles.findExitedBetween(T0, T0.plusSeconds(7200)).size());
        assertEquals(1, vehicles.findByAdminId(partner.getId()).size());
        assertEquals(1, vehicles.findByParkingId(parking.getId()).size());
    }

    @Test
    void rankingsQueries() {
        vehicles.save(Vehicle.enter("AAA111", "M", parking, partner, T0));
        Vehicle second = Vehicle.enter("AAA111", "M", parking, partner, T0.plusSeconds(10));
        second.exit(T0.plusSeconds(20));
        vehicles.save(second);
        vehicles.save(Vehicle.enter("BBB222", "M", parking, partner, T0.plusSeconds(30)));

        List<TopVehicle> top = vehicles.findTopVehicles(10);
        assertEquals(new TopVehicle("AAA111", 2L), top.getFirst());
        assertEquals(2, vehicles.findTopVehiclesByParking(parking.getId(), 10).size());

        List<TopPartner> partners = vehicles.findTopPartners(T0.minusSeconds(1), T0.plusSeconds(60), 3);
        assertEquals(List.of(new TopPartner("socio1", 3L, parking.getId())), partners);
    }

    @Test
    void deleteParking_deletesItsVehicles() {
        Vehicle saved = vehicles.save(Vehicle.enter("ABC123", "Mazda", parking, partner, T0));
        // A delete request runs with a fresh persistence context.
        entityManager.flush();
        entityManager.clear();

        parkings.deleteById(parking.getId());
        entityManager.flush();

        assertTrue(parkings.findById(parking.getId()).isEmpty());
        assertTrue(vehicles.findById(saved.getId()).isEmpty());
    }

    @Test
    void firstTimeParked_onlyReturnsPlatesWithNoPreviousVisit() {
        Vehicle returning = vehicles.save(Vehicle.enter("OLD111", "Mazda", parking, partner, T0));
        returning.exit(T0.plusSeconds(600));
        vehicles.save(returning);
        vehicles.save(Vehicle.enter("OLD111", "Mazda", parking, partner, T0.plusSeconds(3600)));
        vehicles.save(Vehicle.enter("NEW222", "Kia", parking, partner, T0.plusSeconds(3600)));
        Vehicle gone = vehicles.save(Vehicle.enter("GONE33", "Ford", parking, partner, T0));
        gone.exit(T0.plusSeconds(60));
        vehicles.save(gone);
        entityManager.flush();

        List<Vehicle> firstTime = vehicles.findFirstTimeParked(VehicleStatus.IN);

        assertEquals(List.of("NEW222"), firstTime.stream().map(Vehicle::getPlateNumber).toList());
    }
}
