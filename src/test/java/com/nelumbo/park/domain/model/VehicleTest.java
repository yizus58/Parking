package com.nelumbo.park.domain.model;

import com.nelumbo.park.domain.exception.VehicleOutParkingException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    private static final Instant NOW = Instant.parse("2026-01-10T15:00:00Z");

    private final User partner = new User("u1", "socio", "hash", "socio@example.com", "SOCIO");
    private final Parking parking = new Parking("p1", "Parking Centro", "Calle 1 # 2-3", 10, 2500f, partner);

    @Test
    void enter_setsStatusInAndFreezesRate() {
        Vehicle vehicle = Vehicle.enter("ABC123", "Mazda", parking, partner, NOW);

        assertEquals(VehicleStatus.IN, vehicle.getStatus());
        assertEquals(NOW, vehicle.getEntryTime());
        assertNull(vehicle.getExitTime());
        assertEquals(2500f, vehicle.getCostPerHour());
        assertTrue(vehicle.wasRegisteredBy(partner));
        assertNull(vehicle.getId());
    }

    @Test
    void exit_chargesEveryStartedHour() {
        Vehicle vehicle = Vehicle.enter("ABC123", "Mazda", parking, partner, NOW);

        float cost = vehicle.exit(NOW.plus(Duration.ofMinutes(61)));

        assertEquals(5000f, cost);
        assertEquals(VehicleStatus.OUT, vehicle.getStatus());
        assertEquals(NOW.plus(Duration.ofMinutes(61)), vehicle.getExitTime());
    }

    @Test
    void exit_underOneMinuteIsFree() {
        Vehicle vehicle = Vehicle.enter("ABC123", "Mazda", parking, partner, NOW);

        assertEquals(0f, vehicle.exit(NOW.plusSeconds(30)));
    }

    @Test
    void exit_twiceFails() {
        Vehicle vehicle = Vehicle.enter("ABC123", "Mazda", parking, partner, NOW);
        vehicle.exit(NOW.plusSeconds(3600));

        assertThrows(VehicleOutParkingException.class, () -> vehicle.exit(NOW.plusSeconds(7200)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"abc123,ABC123", "' Abc123 ',ABC123", "ABC123,ABC123"})
    void enter_normalizesThePlate(String given, String stored) {
        assertEquals(stored, Vehicle.enter(given, "Mazda", parking, partner, NOW).getPlateNumber());
    }

    @Test
    void normalizePlate_isLocaleIndependentAndNullSafe() {
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertEquals("XIL123", Vehicle.normalizePlate("xil123"));
        } finally {
            java.util.Locale.setDefault(original);
        }
        assertNull(Vehicle.normalizePlate(null));
    }
}
