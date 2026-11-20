package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.Vehicle;

public interface RegisterVehicleExitUseCase {

    VehicleExit registerExit(String plateNumber);

    record VehicleExit(Vehicle vehicle, float totalCost) {
    }
}
