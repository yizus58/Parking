package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.ParkingIndicatorsUseCase;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.ParkingRanking;
import com.nelumbo.park.domain.model.stats.PartnerRanking;
import com.nelumbo.park.domain.model.stats.RankingPeriod;
import com.nelumbo.park.domain.model.stats.TopVehicle;
import com.nelumbo.park.domain.service.ParkingEarningsRanking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ParkingIndicatorsService implements ParkingIndicatorsUseCase {

    static final int TOP_VEHICLES = 10;
    static final int TOP_PARTNERS = 3;

    private final VehicleRepositoryPort vehicleRepository;
    private final Clock clock;

    public ParkingIndicatorsService(VehicleRepositoryPort vehicleRepository, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.clock = clock;
    }

    @Override
    public List<Vehicle> firstTimeParkedVehicles() {
        return vehicleRepository.findFirstTimeParked(VehicleStatus.IN);
    }

    @Override
    public List<TopVehicle> topVehicles() {
        return vehicleRepository.findTopVehicles(TOP_VEHICLES);
    }

    @Override
    public List<TopVehicle> topVehiclesByParking(String parkingId) {
        List<TopVehicle> top = vehicleRepository.findTopVehiclesByParking(parkingId, TOP_VEHICLES);
        if (top.isEmpty()) {
            throw new ParkingNotFoundException();
        }
        return top;
    }

    @Override
    public PartnerRanking partnersRankingOfWeek() {
        return partnerRanking(RankingPeriod.weekOf(now()));
    }

    @Override
    public PartnerRanking partnersRankingOfMonth() {
        return partnerRanking(RankingPeriod.monthOf(now()));
    }

    @Override
    public ParkingRanking parkingRankingOfWeek() {
        return parkingRanking(RankingPeriod.weekOf(now()));
    }

    /**
     * Kept as before: the "month" parking ranking uses the current week.
     */
    @Override
    public ParkingRanking parkingRankingOfMonth() {
        return parkingRanking(RankingPeriod.weekOf(now()));
    }

    private PartnerRanking partnerRanking(RankingPeriod period) {
        return new PartnerRanking(period,
                vehicleRepository.findTopPartners(toInstant(period.start()), toInstant(period.end()), TOP_PARTNERS));
    }

    private ParkingRanking parkingRanking(RankingPeriod period) {
        List<Vehicle> exited = vehicleRepository.findExitedBetween(toInstant(period.start()), toInstant(period.end()));
        return new ParkingRanking(period, ParkingEarningsRanking.top(exited, ParkingEarningsRanking.DEFAULT_LIMIT));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(clock.getZone()).toInstant();
    }
}
