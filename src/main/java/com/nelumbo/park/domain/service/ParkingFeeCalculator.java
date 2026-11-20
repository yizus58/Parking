package com.nelumbo.park.domain.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Parking fee rules: every started hour is charged, measured in whole minutes.
 */
public final class ParkingFeeCalculator {

    private ParkingFeeCalculator() {
    }

    public static long billableHours(Instant entry, Instant exit) {
        long minutes = Duration.between(entry, exit).abs().toMinutes();
        return (minutes + 59) / 60;
    }

    public static float cost(Instant entry, Instant exit, Float costPerHour) {
        float rate = costPerHour != null ? costPerHour : 0.0f;
        return billableHours(entry, exit) * rate;
    }
}
