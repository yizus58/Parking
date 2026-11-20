package com.nelumbo.park.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingFeeCalculatorTest {

    private static final Instant T0 = Instant.parse("2026-01-10T10:00:00Z");

    @Test
    void billableHours_roundsUpStartedHours() {
        assertEquals(0, ParkingFeeCalculator.billableHours(T0, T0.plusSeconds(59)));
        assertEquals(1, ParkingFeeCalculator.billableHours(T0, T0.plusSeconds(60)));
        assertEquals(1, ParkingFeeCalculator.billableHours(T0, T0.plusSeconds(3600)));
        assertEquals(2, ParkingFeeCalculator.billableHours(T0, T0.plusSeconds(3660)));
    }

    @Test
    void cost_multipliesByRate_andToleratesMissingRate() {
        assertEquals(7500f, ParkingFeeCalculator.cost(T0, T0.plusSeconds(3 * 3600), 2500f));
        assertEquals(0f, ParkingFeeCalculator.cost(T0, T0.plusSeconds(3600), null));
    }
}
