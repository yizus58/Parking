package com.nelumbo.park.domain.service;

import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.TopParking;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingEarningsRankingTest {

    private static final Instant T0 = Instant.parse("2026-01-10T10:00:00Z");

    private static Vehicle out(Parking parking, long hours, float rate) {
        return new Vehicle("v" + hours + parking.getId(), "ABC", "M", T0, T0.plusSeconds(hours * 3600), rate,
                VehicleStatus.OUT, parking, null);
    }

    @Test
    void top_sortsByEarningsAndLimits() {
        Parking a = new Parking("a", "A", "addr", 5, 1000f, null);
        Parking b = new Parking("b", "B", "addr", 5, 1000f, null);
        Parking c = new Parking("c", "C", "addr", 5, 1000f, null);
        Parking d = new Parking("d", "D", "addr", 5, 1000f, null);
        Parking free = new Parking("e", "E", "addr", 5, 0f, null);

        List<TopParking> top = ParkingEarningsRanking.top(List.of(
                out(a, 1, 1000f), out(b, 5, 1000f), out(b, 1, 1000f), out(c, 3, 1000f), out(d, 2, 1000f),
                out(free, 2, 0f)), 3);

        assertEquals(List.of("b", "c", "d"), top.stream().map(TopParking::parkingId).toList());
        assertEquals(2L, top.getFirst().totalVehicles());
        assertEquals(6000f, top.getFirst().totalEarnings());
    }

    @Test
    void top_ignoresVehiclesWithoutParkingOrTimes() {
        Vehicle noParking = new Vehicle("x", "ABC", "M", T0, T0.plusSeconds(3600), 1000f, VehicleStatus.OUT, null, null);
        Vehicle inside = new Vehicle("y", "ABC", "M", T0, null, 1000f, VehicleStatus.IN,
                new Parking("a", "A", "addr", 5, 1000f, null), null);

        assertEquals(List.of(), ParkingEarningsRanking.top(List.of(noParking, inside), 3));
    }
}
