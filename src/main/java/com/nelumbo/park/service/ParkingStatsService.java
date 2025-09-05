package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.TopParkingResponse;
import com.nelumbo.park.dto.response.TopPartnerResponse;
import com.nelumbo.park.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.repository.VehicleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class ParkingStatsService {

    private final VehicleRepository vehicleRepository;
    private final ParkingEarningsCalculator earningsCalculator;

    public ParkingStatsService(VehicleRepository vehicleRepository, 
                              ParkingEarningsCalculator earningsCalculator) {
        this.vehicleRepository = vehicleRepository;
        this.earningsCalculator = earningsCalculator;
    }

    public WeeklyPartnerStatsResponse getPartnersRanking() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(DayOfWeek.MONDAY).withHour(5).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime weekEnd = weekStart.plusDays(6).withHour(23).withMinute(59).withSecond(59).withNano(999000000);

        Date startOfWeek = Date.from(weekStart.atZone(ZoneId.systemDefault()).toInstant());
        Date endOfWeek = Date.from(weekEnd.atZone(ZoneId.systemDefault()).toInstant());

        Pageable topThree = PageRequest.of(0, 3);
        List<TopPartnerResponse> topPartners = vehicleRepository.findTopPartnersByWeek(startOfWeek, endOfWeek, topThree);

        return new WeeklyPartnerStatsResponse(weekStart, weekEnd, topPartners);
    }

    public WeeklyParkingStatsResponse getParkingRanking() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(DayOfWeek.MONDAY).withHour(5).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime weekEnd = weekStart.plusDays(6).withHour(23).withMinute(59).withSecond(59).withNano(999000000);

        Date startOfWeek = Date.from(weekStart.atZone(ZoneId.systemDefault()).toInstant());
        Date endOfWeek = Date.from(weekEnd.atZone(ZoneId.systemDefault()).toInstant());

        List<Vehicle> vehicles = vehicleRepository.findVehiclesWithExitTimeBetween(startOfWeek, endOfWeek);

        List<TopParkingResponse> topParkings = earningsCalculator.calculateParkingEarnings(vehicles);

        return new WeeklyParkingStatsResponse(weekStart, weekEnd, topParkings);
    }
}
