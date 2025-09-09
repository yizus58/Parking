package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.TopParkingResponse;
import com.nelumbo.park.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ParkingEarningsCalculator {

    public List<TopParkingResponse> calculateParkingEarnings(List<Vehicle> vehicles) {
        Map<String, ParkingEarnings> parkingEarningsMap = new HashMap<>();

        vehicles.forEach(vehicle -> {
            String parkingId = vehicle.getParking().getId();
            String parkingName = vehicle.getParking().getName();
            Float costPerHour = vehicle.getCostPerHour();

            Date entryTime = vehicle.getEntryTime();
            Date exitTime = vehicle.getExitTime();

            if (entryTime != null && exitTime != null) {
                Float totalCost = calculateVehicleCost(entryTime, exitTime, costPerHour);

                parkingEarningsMap.computeIfAbsent(parkingId, 
                    k -> new ParkingEarnings(parkingId, parkingName, 0.0f, 0L))
                    .addEarnings(totalCost);
            }
        });

        return parkingEarningsMap.values()
                .stream()
                .filter(earnings -> earnings.getTotalEarnings() > 0.0f)
                .sorted((a, b) -> Float.compare(b.getTotalEarnings(), a.getTotalEarnings()))
                .limit(3)
                .map(this::toTopParkingResponse)
                .toList();
    }

    private Float calculateVehicleCost(Date entryTime, Date exitTime, Float costPerHour) {
        long timeDifference = Math.abs(exitTime.getTime() - entryTime.getTime());
        long minutesParked = timeDifference / (1000 * 60);
        long hoursParked = (long) Math.ceil((double) minutesParked / 60);
        return hoursParked * costPerHour;
    }

    private TopParkingResponse toTopParkingResponse(ParkingEarnings earnings) {
        return new TopParkingResponse(
                earnings.getId(),
                earnings.getName(),
                earnings.getVehicleCount(),
                earnings.getTotalEarnings()
        );
    }

    private static class ParkingEarnings {
        private final String id;
        private final String name;
        private Float totalEarnings;
        private Long vehicleCount;

        public ParkingEarnings(String id, String name, Float totalEarnings, Long vehicleCount) {
            this.id = id;
            this.name = name;
            this.totalEarnings = totalEarnings;
            this.vehicleCount = vehicleCount;
        }

        public void addEarnings(Float amount) {
            this.totalEarnings += amount;
            this.vehicleCount += 1;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Float getTotalEarnings() { return totalEarnings; }
        public Long getVehicleCount() { return vehicleCount; }
    }
}
