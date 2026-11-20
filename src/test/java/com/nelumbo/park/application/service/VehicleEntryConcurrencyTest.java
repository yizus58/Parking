package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase;
import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase.VehicleEntryCommand;
import com.nelumbo.park.application.port.out.CurrentUserPort;
import com.nelumbo.park.domain.exception.LimitParkingFullException;
import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.infrastructure.adapter.out.persistence.LoginLogPersistenceAdapter;
import com.nelumbo.park.infrastructure.adapter.out.persistence.ParkingPersistenceAdapter;
import com.nelumbo.park.infrastructure.adapter.out.persistence.UserPersistenceAdapter;
import com.nelumbo.park.infrastructure.adapter.out.persistence.VehiclePersistenceAdapter;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.ParkingJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.VehicleJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real transactions against the embedded database: concurrent entries must
 * never put more vehicles in a parking than its capacity.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({VehicleEntryService.class, CurrentUserResolver.class, UserPersistenceAdapter.class,
        ParkingPersistenceAdapter.class, VehiclePersistenceAdapter.class, LoginLogPersistenceAdapter.class,
        VehicleEntryConcurrencyTest.Config.class})
class VehicleEntryConcurrencyTest {

    private static final int CAPACITY = 3;
    private static final int THREADS = 8;
    private static final AtomicReference<User> CURRENT = new AtomicReference<>();

    @TestConfiguration
    static class Config {
        @Bean
        CurrentUserPort currentUserPort() {
            return () -> Optional.ofNullable(CURRENT.get());
        }

        @Bean
        Clock clock() {
            return Clock.systemDefaultZone();
        }
    }

    @Autowired
    private RegisterVehicleEntryUseCase registerEntry;
    @Autowired
    private UserPersistenceAdapter users;
    @Autowired
    private ParkingPersistenceAdapter parkings;
    @Autowired
    private VehiclePersistenceAdapter vehicles;
    @Autowired
    private VehicleJpaRepository vehicleJpa;
    @Autowired
    private ParkingJpaRepository parkingJpa;
    @Autowired
    private UserJpaRepository userJpa;

    @AfterEach
    void cleanUp() {
        vehicleJpa.deleteAll();
        parkingJpa.deleteAll();
        userJpa.deleteAll();
    }

    private Parking givenParking() {
        User owner = users.save(User.newPartner("socio-conc", "conc@example.com", "hash"));
        CURRENT.set(owner);
        return parkings.save(new Parking(null, "Parking Concurrente", "Calle 1 # 2-3", CAPACITY, 1000f, owner));
    }

    private List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return task.call();
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentEntriesNeverExceedCapacity() throws Exception {
        Parking parking = givenParking();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String plate = "CON" + i;
            tasks.add(() -> registerEntry.registerEntry(new VehicleEntryCommand(plate, "Model", parking.getId())));
        }

        List<Object> results = runConcurrently(tasks);

        long full = results.stream().filter(r -> r instanceof LimitParkingFullException).count();
        long other = results.stream().filter(r -> r instanceof RuntimeException && !(r instanceof LimitParkingFullException)).count();
        assertEquals(0, other, () -> "unexpected errors: " + results);
        assertEquals(THREADS - CAPACITY, full);
        assertEquals(CAPACITY, vehicles.countByParkingIdAndStatus(parking.getId(), VehicleStatus.IN));
    }

    @Test
    void concurrentEntriesOfTheSamePlateIntoOneParkingRegisterOnce() throws Exception {
        Parking parking = givenParking();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> registerEntry.registerEntry(new VehicleEntryCommand("SAME1", "Model", parking.getId())));
        }

        List<Object> results = runConcurrently(tasks);

        assertTrue(results.stream().filter(r -> r instanceof RuntimeException)
                .allMatch(r -> r instanceof VehicleAlreadyInParkingException), () -> results.toString());
        assertEquals(1, vehicles.countByParkingIdAndStatus(parking.getId(), VehicleStatus.IN));
    }
}
