package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase.VehicleEntryCommand;
import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.support.RequiresPostgres;
import com.nelumbo.park.support.TestPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The concurrency scenarios of {@link VehicleEntryConcurrencyTest} on real
 * PostgreSQL (row locks, schema from the Flyway migrations), plus the case
 * only the database can enforce: the same plate entering two different
 * parkings at the same time.
 */
@RequiresPostgres
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VehicleEntryPostgresConcurrencyTest extends VehicleEntryConcurrencyTest {

    private static final int ROUNDS = 25;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        TestPostgres.registerProperties(registry);
    }

    @Test
    void samePlateIntoTwoParkingsAtOnce_entersOnlyOnce() throws Exception {
        User owner = users.save(User.newPartner("socio-two", "two@example.com", "hash"));
        CURRENT.set(owner);
        Parking north = parkings.save(new Parking(null, "Norte", "Calle 100 # 1-1", 50, 1000f, owner));
        Parking south = parkings.save(new Parking(null, "Sur", "Calle 1 # 1-1", 50, 1000f, owner));

        for (int round = 0; round < ROUNDS; round++) {
            String plate = "TWO" + round;
            List<Callable<Object>> tasks = new ArrayList<>();
            tasks.add(() -> registerEntry.registerEntry(new VehicleEntryCommand(plate, "Model", north.getId())));
            tasks.add(() -> registerEntry.registerEntry(new VehicleEntryCommand(plate, "Model", south.getId())));

            List<Object> results = runConcurrently(tasks);

            List<Object> errors = results.stream().filter(r -> r instanceof RuntimeException).toList();
            assertEquals(1, errors.size(), () -> "round " + plate + ": " + results);
            assertTrue(errors.getFirst() instanceof VehicleAlreadyInParkingException, () -> results.toString());
            assertEquals("El vehiculo con placa " + plate + " ya está registrado y actualmente en un parking",
                    ((RuntimeException) errors.getFirst()).getMessage());
        }
        long inside = vehicles.countByParkingIdAndStatus(north.getId(), VehicleStatus.IN)
                + vehicles.countByParkingIdAndStatus(south.getId(), VehicleStatus.IN);
        assertEquals(ROUNDS, inside);
    }
}
