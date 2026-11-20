package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;

import java.time.Instant;

final class WebFixtures {

    /** 2026-11-20 09:15 in Bogota. */
    static final Instant ENTRY = Instant.parse("2026-11-20T14:15:00Z");
    static final Instant EXIT = Instant.parse("2026-11-20T16:20:00Z");

    private WebFixtures() {
    }

    static User owner() {
        return new User("owner-1", "socio", "hash", "socio@example.com", "SOCIO");
    }

    static Parking parking() {
        return new Parking("park-1", "Parking Centro", "Calle 10 # 20-30", 20, 2500f, owner());
    }

    static Vehicle inside() {
        return new Vehicle("veh-1", "ABC123", "Mazda", ENTRY, null, 2500f, VehicleStatus.IN, parking(), owner());
    }

    static Vehicle out() {
        return new Vehicle("veh-1", "ABC123", "Mazda", ENTRY, EXIT, 2500f, VehicleStatus.OUT, parking(), owner());
    }
}
