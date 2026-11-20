package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.ParkingRanking;
import com.nelumbo.park.domain.model.stats.PartnerRanking;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.domain.model.stats.TopVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParkingIndicatorsServiceTest {

    private VehicleRepositoryPort vehicles;
    private ParkingIndicatorsService service;

    @BeforeEach
    void setUp() {
        vehicles = mock(VehicleRepositoryPort.class);
        service = new ParkingIndicatorsService(vehicles, Fixtures.CLOCK);
    }

    @Test
    void topVehicles_top10() {
        when(vehicles.findTopVehicles(10)).thenReturn(List.of(new TopVehicle("ABC123", 4L)));
        assertEquals(4L, service.topVehicles().getFirst().totalVisits());
    }

    @Test
    void topVehiclesByParking_emptyMeansNotFound() {
        when(vehicles.findTopVehiclesByParking("park-1", 10)).thenReturn(List.of());
        assertThrows(ParkingNotFoundException.class, () -> service.topVehiclesByParking("park-1"));
    }

    @Test
    void firstTimeParked_usesStatusIn() {
        service.firstTimeParkedVehicles();
        verify(vehicles).findFirstTimeParked(VehicleStatus.IN);
    }

    @Test
    void partnersWeek_usesBogotaWeekWindowAndTop3() {
        Instant from = Instant.parse("2026-11-16T10:00:00Z"); // Monday 05:00 in Bogota
        Instant to = Instant.parse("2026-11-23T04:59:59.999Z"); // Sunday 23:59:59.999 in Bogota
        List<TopPartner> top = List.of(new TopPartner("socio", 3L, "park-1"));
        when(vehicles.findTopPartners(from, to, 3)).thenReturn(top);

        PartnerRanking ranking = service.partnersRankingOfWeek();

        assertEquals(LocalDateTime.of(2026, 11, 16, 5, 0), ranking.period().start());
        assertEquals(top, ranking.topPartners());
    }

    @Test
    void partnersMonth_startsDayOne() {
        PartnerRanking ranking = service.partnersRankingOfMonth();
        assertEquals(LocalDateTime.of(2026, 11, 1, 5, 0), ranking.period().start());
        verify(vehicles).findTopPartners(Instant.parse("2026-11-01T10:00:00Z"),
                Instant.parse("2026-12-02T04:59:59.999Z"), 3);
    }

    @Test
    void parkingWeek_ranksByEarnings() {
        Parking parking = Fixtures.parking("park-1", Fixtures.partner("p1"), 5);
        Vehicle out = new Vehicle("v1", "ABC123", "M", Fixtures.NOW.minusSeconds(7200), Fixtures.NOW, 1000f,
                VehicleStatus.OUT, parking, null);
        when(vehicles.findExitedBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(out));

        ParkingRanking ranking = service.parkingRankingOfWeek();

        assertEquals(1, ranking.topParkings().size());
        assertEquals(2000f, ranking.topParkings().getFirst().totalEarnings());
    }

    @Test
    void parkingMonth_keepsWeekWindow() {
        assertEquals(LocalDateTime.of(2026, 11, 16, 5, 0), service.parkingRankingOfMonth().period().start());
    }
}
