package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.Vehicle;

import java.util.List;

public interface ManageVehiclesUseCase {

    List<Vehicle> listVehicles();

    Vehicle getVehicle(String id);

    void deleteVehicle(String id);
}
