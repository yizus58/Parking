package com.nelumbo.park.domain.service;

import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.stats.TopParking;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ranks parkings by the money earned from the given vehicles.
 */
public final class ParkingEarningsRanking {

    public static final int DEFAULT_LIMIT = 3;

    private ParkingEarningsRanking() {
    }

    public static List<TopParking> top(List<Vehicle> vehicles, int limit) {
        Map<String, Accumulator> byParking = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getParking() == null || vehicle.getEntryTime() == null || vehicle.getExitTime() == null) {
                continue;
            }
            float cost = ParkingFeeCalculator.cost(vehicle.getEntryTime(), vehicle.getExitTime(), vehicle.getCostPerHour());
            byParking.computeIfAbsent(vehicle.getParking().getId(),
                            id -> new Accumulator(id, vehicle.getParking().getName()))
                    .add(cost);
        }
        return byParking.values().stream()
                .filter(acc -> acc.earnings > 0.0f)
                .sorted(Comparator.comparingDouble((Accumulator acc) -> acc.earnings).reversed())
                .limit(limit)
                .map(acc -> new TopParking(acc.parkingId, acc.parkingName, acc.vehicles, acc.earnings))
                .toList();
    }

    private static final class Accumulator {
        private final String parkingId;
        private final String parkingName;
        private float earnings;
        private long vehicles;

        private Accumulator(String parkingId, String parkingName) {
            this.parkingId = parkingId;
            this.parkingName = parkingName;
        }

        private void add(float cost) {
            earnings += cost;
            vehicles++;
        }
    }
}
