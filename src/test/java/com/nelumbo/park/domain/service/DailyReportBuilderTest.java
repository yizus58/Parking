package com.nelumbo.park.domain.service;

import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DailyReportBuilderTest {

    private static final Instant EXIT = Instant.parse("2026-11-20T18:30:00Z");

    private final User u1 = new User("u1", "socio1", "h", "s1@example.com", "SOCIO");
    private final User u2 = new User("u2", "socio2", "h", "s2@example.com", "SOCIO");
    private final Parking p1 = new Parking("p1", "Centro", "addr", 5, 1000f, u1);
    private final Parking p2 = new Parking("p2", "Norte", "addr", 5, 1000f, u1);
    private final Parking p3 = new Parking("p3", "Sur", "addr", 5, 1000f, u2);

    private Vehicle out(String id, Parking parking, User admin, long minutes, Float rate) {
        return new Vehicle(id, "PLATE" + id, "Model" + id, EXIT.minusSeconds(minutes * 60), EXIT, rate,
                VehicleStatus.OUT, parking, admin);
    }

    @Test
    void groupsByPartnerAndParking_sortedByEarnings() {
        List<VehicleOutDetail> reports = DailyReportBuilder.build(List.of(
                out("a", p1, u1, 30, 1000f),
                out("b", p1, u1, 121, 1000f),
                out("c", p2, u1, 60, 1000f),
                out("d", p3, u2, 600, 1000f)));

        assertEquals(List.of("p3", "p1", "p2"), reports.stream().map(VehicleOutDetail::getParkingId).toList());
        VehicleOutDetail centro = reports.get(1);
        assertEquals("u1", centro.getUserId());
        assertEquals("socio1", centro.getUsername());
        assertEquals("s1@example.com", centro.getEmail());
        assertEquals("Centro", centro.getParking());
        assertEquals(2, centro.getTotalVehicles());
        assertEquals(4000f, centro.getTotalEarnings());
        assertEquals("b", centro.getVehicles().getFirst().getVehicleId());
        assertEquals(3000f, centro.getVehicles().getFirst().getTotalCost());
        assertEquals("20-11-2026-18:30", centro.getVehicles().getFirst().getDayExit());
        assertEquals("20-11-2026-16:29", centro.getVehicles().getFirst().getDayEntry());
    }

    @Test
    void nullRateCostsZero() {
        VehicleOutDetail report = DailyReportBuilder.build(List.of(out("a", p1, u1, 90, null))).getFirst();
        assertEquals(0f, report.getTotalEarnings());
    }

    @Test
    void empty() {
        assertTrue(DailyReportBuilder.build(List.of()).isEmpty());
    }
}
