package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.ParkingEarningsResponse;
import com.nelumbo.park.dto.response.TopParkingResponse;
import com.nelumbo.park.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ParkingEarningsCalculator {

    public List<TopParkingResponse> calculateParkingEarnings(List<Vehicle> vehicles) {
        Map<String, ParkingEarningsResponse> parkingEarningsMap = new HashMap<>();

        vehicles.forEach(vehicle -> {
            String parkingId = vehicle.getParking().getId();
            String parkingName = vehicle.getParking().getName();
            Float costPerHour = vehicle.getCostPerHour();

            Date entryTime = vehicle.getEntryTime();
            Date exitTime = vehicle.getExitTime();

            if (entryTime != null && exitTime != null) {
                Float totalCost = calculateVehicleCost(entryTime, exitTime, costPerHour);

                parkingEarningsMap.computeIfAbsent(parkingId,
                    k -> new ParkingEarningsResponse(parkingId, parkingName, 0.0f, 0L))
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

    private TopParkingResponse toTopParkingResponse(ParkingEarningsResponse earnings) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
        String formattedCost = currencyFormatter.format(earnings.getTotalEarnings()).replace("COP", "$").replace(",00", "");

        return new TopParkingResponse(
                earnings.getId(),
                earnings.getName(),
                earnings.getVehicleCount(),
                formattedCost
        );
    }

}
