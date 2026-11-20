package com.nelumbo.park.application.service;

import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

final class Fixtures {

    static final Instant NOW = Instant.parse("2026-11-20T19:42:00Z");
    static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    static final Clock CLOCK = Clock.fixed(NOW, BOGOTA);

    private Fixtures() {
    }

    static User admin() {
        return new User("admin-1", "admin", "hash", "admin@example.com", "ADMIN");
    }

    static User partner(String id) {
        return new User(id, "socio-" + id, "hash", id + "@example.com", "SOCIO");
    }

    static Parking parking(String id, User owner, int capacity) {
        return new Parking(id, "Parking " + id, "Calle 10 # 20-30", capacity, 2000f, owner);
    }

    static Vehicle inside(String id, String plate, Parking parking, User admin) {
        return new Vehicle(id, plate, "Mazda", NOW.minusSeconds(3600), null, parking.getCostPerHour(),
                VehicleStatus.IN, parking, admin);
    }
}
