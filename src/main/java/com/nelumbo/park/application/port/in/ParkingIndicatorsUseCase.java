package com.nelumbo.park.application.port.in;

import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.stats.ParkingRanking;
import com.nelumbo.park.domain.model.stats.PartnerRanking;
import com.nelumbo.park.domain.model.stats.TopVehicle;

import java.util.List;

public interface ParkingIndicatorsUseCase {

    List<Vehicle> firstTimeParkedVehicles();

    List<TopVehicle> topVehicles();

    List<TopVehicle> topVehiclesByParking(String parkingId);

    PartnerRanking partnersRankingOfWeek();

    PartnerRanking partnersRankingOfMonth();

    ParkingRanking parkingRankingOfWeek();

    ParkingRanking parkingRankingOfMonth();
}
