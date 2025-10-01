package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.VehicleDetailResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class VehicleReportService {

    private final VehicleRepository vehicleRepository;
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    public VehicleReportService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public static String formatDate(Date date, boolean dateOnly) {
        String pattern = dateOnly ? "dd-MM-yyyy" : "dd-MM-yyyy-HH:mm";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.setTimeZone(UTC_TIME_ZONE);
        return formatter.format(date);
    }

    public List<VehicleOutDetailResponse> getVehiclesOutDetails() {
        List<Vehicle> vehiclesOut = getVehiclesOutToday();
        return processVehiclesOutDetails(vehiclesOut);
    }

    private List<Vehicle> getVehiclesOutToday() {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        return vehicleRepository.findVehiclesWithExitTimeBetween(
                        startOfDay.getTime(),
                        endOfDay.getTime()
                ).stream()
                .filter(vehicle -> VehicleStatus.OUT.equals(vehicle.getStatus()))
                .toList();
    }

    private List<VehicleOutDetailResponse> processVehiclesOutDetails(List<Vehicle> vehiclesOut) {
        Map<String, Map<String, Map<String, VehicleDetailResponse>>> parkingEarnings = new HashMap<>();

        for (Vehicle vehicle : vehiclesOut) {
            String vehicleId = vehicle.getId();
            String parkingId = vehicle.getParking().getId();
            String userId = vehicle.getAdmin().getId();

            Float costPerHour = vehicle.getCostPerHour() != null ? vehicle.getCostPerHour() : 0.0f;
            String plateNumber = vehicle.getPlateNumber();
            String modelVehicle = vehicle.getModel();

            Date entryTime = vehicle.getEntryTime();
            Date exitTime = vehicle.getExitTime();

            long timeDifference = Math.abs(exitTime.getTime() - entryTime.getTime());
            long minutesParked = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
            long hoursParked = (minutesParked + 59) / 60;

            Float totalCost = hoursParked * costPerHour;
            String dayEntry = formatDate(entryTime, false);
            String dayExit = formatDate(exitTime, false);

            parkingEarnings.computeIfAbsent(userId, k -> new HashMap<>());

            if (!parkingEarnings.get(userId).containsKey(parkingId)) {
                parkingEarnings.get(userId).put(parkingId, new HashMap<>());
            }

            VehicleDetailResponse vehicleDetail = new VehicleDetailResponse(
                    vehicleId,
                    plateNumber,
                    modelVehicle,
                    dayEntry,
                    dayExit,
                    totalCost
            );

            parkingEarnings.get(userId).get(parkingId).put(vehicleId, vehicleDetail);
        }

        return flattenVehicleData(parkingEarnings, vehiclesOut);
    }

    private List<VehicleOutDetailResponse> flattenVehicleData(
            Map<String, Map<String, Map<String, VehicleDetailResponse>>> parkingEarnings,
            List<Vehicle> vehiclesOut) {

        List<VehicleOutDetailResponse> flattenedData = new ArrayList<>();

        for (Map.Entry<String, Map<String, Map<String, VehicleDetailResponse>>> userEntry : parkingEarnings.entrySet()) {
            String userId = userEntry.getKey();
            Map<String, Map<String, VehicleDetailResponse>> userParkings = userEntry.getValue();

            Vehicle sampleVehicle = vehiclesOut.stream()
                    .filter(v -> v.getAdmin() != null && v.getAdmin().getId() != null && v.getAdmin().getId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (sampleVehicle == null) continue;

            String username = sampleVehicle.getAdmin().getUsername();
            String email = sampleVehicle.getAdmin().getEmail();

            for (Map.Entry<String, Map<String, VehicleDetailResponse>> parkingEntry : userParkings.entrySet()) {
                String parkingId = parkingEntry.getKey();
                Map<String, VehicleDetailResponse> parkingVehicles = parkingEntry.getValue();

                String parkingName = vehiclesOut.stream()
                        .filter(v -> v.getParking() != null && v.getParking().getId() != null && v.getParking().getId().equals(parkingId))
                        .map(v -> v.getParking().getName())
                        .findFirst()
                        .orElse("Unknown Parking");

                List<VehicleDetailResponse> vehicles = new ArrayList<>(parkingVehicles.values());
                vehicles.sort((a, b) -> Float.compare(b.getTotalCost(), a.getTotalCost()));

                Float totalEarnings = vehicles.stream()
                        .map(VehicleDetailResponse::getTotalCost)
                        .reduce(0.0f, Float::sum);

                VehicleOutDetailResponse response = new VehicleOutDetailResponse(
                        userId,
                        username,
                        email,
                        parkingId,
                        parkingName,
                        vehicles,
                        vehicles.size(),
                        totalEarnings
                );

                flattenedData.add(response);
            }
        }

        flattenedData.sort((a, b) -> Float.compare(b.getTotalEarnings(), a.getTotalEarnings()));

        return flattenedData;
    }
}