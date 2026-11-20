package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.Vehicle;

public interface RegisterVehicleEntryUseCase {

    Vehicle registerEntry(VehicleEntryCommand command);

    record VehicleEntryCommand(String plateNumber, String model, String parkingId) {
    }
}
