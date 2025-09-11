package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.TopParkingResponse;
import com.nelumbo.park.dto.response.TopPartnerResponse;
import com.nelumbo.park.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingStatsServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ParkingEarningsCalculator earningsCalculator;

    @InjectMocks
    private ParkingStatsService parkingStatsService;

    @Test
    void getPartnersRanking_ShouldReturnWeeklyPartnerStatsResponse() {
        List<TopPartnerResponse> mockTopPartners = Collections.singletonList(new TopPartnerResponse("partner1", 100L, "1"));
        when(vehicleRepository.findTopPartnersByWeek(any(Date.class), any(Date.class), any(Pageable.class)))
                .thenReturn(mockTopPartners);

        WeeklyPartnerStatsResponse response = parkingStatsService.getPartnersRanking();

        assertNotNull(response);
        assertNotNull(response.getWeekStart());
        assertNotNull(response.getWeekEnd());
        assertEquals(mockTopPartners, response.getTopPartners());
    }

    @Test
    void getParkingRanking_ShouldReturnWeeklyParkingStatsResponse() {
        List<Vehicle> mockVehicles = Collections.singletonList(new Vehicle());

        List<TopParkingResponse> mockTopParkings = Collections.singletonList(new TopParkingResponse("1", "Parking Name 1", 10L, 200.0f));

        when(vehicleRepository.findVehiclesWithExitTimeBetween(any(Date.class), any(Date.class)))
                .thenReturn(mockVehicles);
        when(earningsCalculator.calculateParkingEarnings(mockVehicles))
                .thenReturn(mockTopParkings);

        WeeklyParkingStatsResponse response = parkingStatsService.getParkingRanking();

        assertNotNull(response);
        assertNotNull(response.getWeekStart());
        assertNotNull(response.getWeekEnd());
        assertEquals(mockTopParkings, response.getTopParking());
    }
}
