package com.nelumbo.park.domain.model;

import com.nelumbo.park.domain.exception.VehicleOutParkingException;
import com.nelumbo.park.domain.service.ParkingFeeCalculator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    private String id;
    private String plateNumber;
    private String model;
    private Instant entryTime;
    private Instant exitTime;
    private Float costPerHour;
    private VehicleStatus status;
    private Parking parking;
    /** Partner (SOCIO) who registered the entry. */
    private User admin;

    /**
     * A vehicle entering a parking: status IN, no exit time and the parking's
     * current hourly rate frozen on the vehicle.
     */
    public static Vehicle enter(String plateNumber, String model, Parking parking, User admin, Instant now) {
        return new Vehicle(null, plateNumber, model, now, null, parking.getCostPerHour(),
                VehicleStatus.IN, parking, admin);
    }

    public boolean isInside() {
        return status == VehicleStatus.IN;
    }

    public boolean wasRegisteredBy(User user) {
        return admin != null && admin.hasSameIdAs(user);
    }

    /**
     * Registers the exit and returns the amount to charge.
     *
     * @throws VehicleOutParkingException when the vehicle already left
     */
    public float exit(Instant now) {
        if (!isInside()) {
            throw new VehicleOutParkingException();
        }
        this.exitTime = now;
        this.status = VehicleStatus.OUT;
        return ParkingFeeCalculator.cost(entryTime, exitTime, costPerHour);
    }
}
