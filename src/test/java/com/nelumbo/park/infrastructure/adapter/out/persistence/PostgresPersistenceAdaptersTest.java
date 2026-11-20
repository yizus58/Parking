package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.support.RequiresPostgres;
import com.nelumbo.park.support.TestPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every persistence scenario of {@link PersistenceAdaptersTest} again on real
 * PostgreSQL, with the schema created by the Flyway migrations and checked by
 * Hibernate (ddl-auto=validate), plus the PostgreSQL-only constraints.
 */
@RequiresPostgres
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresPersistenceAdaptersTest extends PersistenceAdaptersTest {

    private static final Instant T1 = Instant.parse("2026-02-01T12:00:00Z");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        TestPostgres.registerProperties(registry);
    }

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UserPersistenceAdapter userAdapter;
    @Autowired
    private ParkingPersistenceAdapter parkingAdapter;
    @Autowired
    private VehiclePersistenceAdapter vehicleAdapter;

    @Test
    void migrationsCreatedTheSchemaHibernateValidates() {
        List<String> applied = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertTrue(applied.containsAll(List.of("1", "2", "3")), applied::toString);
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from pg_indexes where indexname = 'ux_vehicles_plate_inside'", Integer.class));
    }

    @Test
    void samePlateCannotBeInsideTwice_evenInAnotherParking() {
        User owner = userAdapter.save(User.newPartner("socio-pg", "pg@example.com", "hash"));
        Parking a = parkingAdapter.save(new Parking(null, "A", "Calle 1", 5, 1000f, owner));
        Parking b = parkingAdapter.save(new Parking(null, "B", "Calle 2", 5, 1000f, owner));
        vehicleAdapter.save(Vehicle.enter("DUP123", "Mazda", a, owner, T1));

        VehicleAlreadyInParkingException e = assertThrows(VehicleAlreadyInParkingException.class,
                () -> vehicleAdapter.save(Vehicle.enter("DUP123", "Mazda", b, owner, T1.plusSeconds(5))));
        assertEquals("El vehiculo con placa DUP123 ya está registrado y actualmente en un parking", e.getMessage());
    }

    @Test
    void samePlateMayEnterAgainAfterLeaving() {
        User owner = userAdapter.save(User.newPartner("socio-pg2", "pg2@example.com", "hash"));
        Parking a = parkingAdapter.save(new Parking(null, "A", "Calle 1", 5, 1000f, owner));
        Vehicle first = vehicleAdapter.save(Vehicle.enter("AGAIN1", "Mazda", a, owner, T1));
        first.exit(T1.plusSeconds(600));
        vehicleAdapter.save(first);

        vehicleAdapter.save(Vehicle.enter("AGAIN1", "Mazda", a, owner, T1.plusSeconds(700)));

        assertEquals(1, vehicleAdapter.countByParkingIdAndStatus(a.getId(), VehicleStatus.IN));
        assertEquals(2, vehicleAdapter.findByParkingId(a.getId()).size());
    }
}
