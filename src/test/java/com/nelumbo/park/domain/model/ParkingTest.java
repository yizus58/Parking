package com.nelumbo.park.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkingTest {

    private final User owner = new User("u1", "socio", "hash", "socio@example.com", "SOCIO");
    private final Parking parking = new Parking("p1", "Parking Centro", "Calle 1 # 2-3", 2, 1000f, owner);

    @Test
    void isOwnedBy_comparesIds() {
        assertTrue(parking.isOwnedBy(new User("u1", null, null, null, null)));
        assertFalse(parking.isOwnedBy(new User("u2", null, null, null, null)));
        assertFalse(parking.isOwnedBy(null));
    }

    @Test
    void isFull_whenInsideReachesCapacity() {
        assertFalse(parking.isFull(0));
        assertFalse(parking.isFull(1));
        assertTrue(parking.isFull(2));
        assertTrue(parking.isFull(3));
    }

    @Test
    void roles() {
        assertTrue(owner.isPartner());
        assertFalse(owner.isAdmin());
        assertTrue(new User("a", "admin", "h", "a@example.com", "ADMIN").isAdmin());
        assertEquals("SOCIO", User.newPartner("n", "e@example.com", "h").getRole());
    }
}
