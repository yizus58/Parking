package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.Vehicle;

import java.util.List;

public interface ManageParkingsUseCase {

    /** Admins see every parking with its owner; partners see their own, without owner. */
    VisibleParkings listParkings();

    ParkingDetails getParking(String id);

    Parking createParking(ParkingCommand command);

    Parking updateParking(String id, ParkingCommand command);

    void deleteParking(String id);

    record ParkingCommand(String name, String address, int capacity, Float costPerHour, String ownerId) {
    }

    record VisibleParkings(List<Parking> parkings, boolean ownerVisible) {
    }

    record ParkingDetails(Parking parking, List<Vehicle> vehicles) {
    }
}
