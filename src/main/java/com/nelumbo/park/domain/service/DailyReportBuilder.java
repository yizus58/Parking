package com.nelumbo.park.domain.service;

import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.report.VehicleDetail;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups exited vehicles by partner and parking for the daily report.
 */
public final class DailyReportBuilder {

    /** Dates in the report are printed in UTC, as before. */
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm").withZone(ZoneOffset.UTC);
    private static final String UNKNOWN_PARKING = "Unknown Parking";

    private DailyReportBuilder() {
    }

    public static String formatDateTime(Instant instant) {
        return DATE_TIME.format(instant);
    }

    /**
     * @param exitedVehicles vehicles with entry and exit time, a parking and an admin
     * @return one report per partner and parking, highest earnings first
     */
    public static List<VehicleOutDetail> build(List<Vehicle> exitedVehicles) {
        Map<String, Map<String, List<VehicleDetail>>> byUserAndParking = new LinkedHashMap<>();
        Map<String, Vehicle> sampleByUser = new LinkedHashMap<>();
        Map<String, String> parkingNames = new LinkedHashMap<>();

        for (Vehicle vehicle : exitedVehicles) {
            String userId = vehicle.getAdmin().getId();
            String parkingId = vehicle.getParking().getId();
            sampleByUser.putIfAbsent(userId, vehicle);
            parkingNames.putIfAbsent(parkingId, vehicle.getParking().getName());

            float totalCost = ParkingFeeCalculator.cost(vehicle.getEntryTime(), vehicle.getExitTime(), vehicle.getCostPerHour());
            VehicleDetail detail = new VehicleDetail(vehicle.getId(), vehicle.getPlateNumber(), vehicle.getModel(),
                    formatDateTime(vehicle.getEntryTime()), formatDateTime(vehicle.getExitTime()), totalCost);
            byUserAndParking.computeIfAbsent(userId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(parkingId, k -> new ArrayList<>())
                    .add(detail);
        }

        List<VehicleOutDetail> reports = new ArrayList<>();
        byUserAndParking.forEach((userId, parkings) -> {
            Vehicle sample = sampleByUser.get(userId);
            parkings.forEach((parkingId, details) -> {
                List<VehicleDetail> vehicles = new ArrayList<>(details);
                vehicles.sort(Comparator.comparing(VehicleDetail::getTotalCost).reversed());
                float totalEarnings = vehicles.stream().map(VehicleDetail::getTotalCost).reduce(0.0f, Float::sum);
                reports.add(new VehicleOutDetail(userId, sample.getAdmin().getUsername(), sample.getAdmin().getEmail(),
                        parkingId, parkingNames.getOrDefault(parkingId, UNKNOWN_PARKING), vehicles, vehicles.size(),
                        totalEarnings));
            });
        });
        reports.sort(Comparator.comparing(VehicleOutDetail::getTotalEarnings).reversed());
        return reports;
    }
}
