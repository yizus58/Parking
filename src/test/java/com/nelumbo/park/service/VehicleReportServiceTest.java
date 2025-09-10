package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.VehicleDetailResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleReportServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleReportService vehicleReportService;

    @Test
    void formatDate_ShouldReturnFormattedDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 15, 10, 30, 0);
        Date date = cal.getTime();

        String formattedDateOnly = VehicleReportService.formatDate(date, true);
        assertEquals("15-01-2023", formattedDateOnly);

        String formattedDateTime = VehicleReportService.formatDate(date, false);
        assertEquals("15-01-2023-10:30", formattedDateTime);
    }

    @Test
    void getVehiclesOutDetails_ShouldReturnCorrectReport() {
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setId("v1");
        vehicle1.setPlateNumber("ABC-123");
        vehicle1.setModel("Model A");
        vehicle1.setCostPerHour(10.0f);
        vehicle1.setStatus(VehicleStatus.OUT);

        Calendar entryCal1 = Calendar.getInstance();
        entryCal1.set(2023, Calendar.JANUARY, 15, 8, 0, 0);
        vehicle1.setEntryTime(entryCal1.getTime());

        Calendar exitCal1 = Calendar.getInstance();
        exitCal1.set(2023, Calendar.JANUARY, 15, 10, 0, 0);
        vehicle1.setExitTime(exitCal1.getTime());

        Parking parking1 = new Parking();
        parking1.setId("p1");
        parking1.setName("Parking One");
        vehicle1.setParking(parking1);

        User admin1 = new User();
        admin1.setId("u1");
        admin1.setUsername("adminUser");
        admin1.setEmail("admin@example.com");
        vehicle1.setAdmin(admin1);

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setId("v2");
        vehicle2.setPlateNumber("DEF-456");
        vehicle2.setModel("Model B");
        vehicle2.setCostPerHour(5.0f);
        vehicle2.setStatus(VehicleStatus.OUT);

        Calendar entryCal2 = Calendar.getInstance();
        entryCal2.set(2023, Calendar.JANUARY, 15, 9, 0, 0);
        vehicle2.setEntryTime(entryCal2.getTime());

        Calendar exitCal2 = Calendar.getInstance();
        exitCal2.set(2023, Calendar.JANUARY, 15, 11, 0, 0);
        vehicle2.setExitTime(exitCal2.getTime());

        Parking parking2 = new Parking();
        parking2.setId("p2");
        parking2.setName("Parking Two");
        vehicle2.setParking(parking2);

        User admin2 = new User();
        admin2.setId("u2");
        admin2.setUsername("adminUser2");
        admin2.setEmail("admin2@example.com");
        vehicle2.setAdmin(admin2);

        List<Vehicle> mockVehicles = Arrays.asList(vehicle1, vehicle2);

        when(vehicleRepository.findVehiclesWithExitTimeBetween(any(Date.class), any(Date.class)))
                .thenReturn(mockVehicles);

        List<VehicleOutDetailResponse> result = vehicleReportService.getVehiclesOutDetails();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());

        VehicleOutDetailResponse report1 = result.stream()
                .filter(r -> r.getUserId().equals("u1"))
                .findFirst().orElse(null);
        assertNotNull(report1);
        assertEquals("u1", report1.getUserId());
        assertEquals("adminUser", report1.getUsername());
        assertEquals("admin@example.com", report1.getEmail());
        assertEquals("p1", report1.getParkingId());
        assertEquals("Parking One", report1.getParking());
        assertEquals(1, report1.getVehicles().size());
        assertEquals(1, report1.getTotalVehicles());
        assertEquals(20.0f, report1.getTotalEarnings());

        VehicleDetailResponse detail1 = report1.getVehicles().get(0);
        assertEquals("v1", detail1.getVehicleId());
        assertEquals("ABC-123", detail1.getPlateNumber());
        assertEquals("Model A", detail1.getModelVehicle());
        assertEquals("15-01-2023-10:00", detail1.getDay());
        assertEquals(20.0f, detail1.getTotalCost());

        VehicleOutDetailResponse report2 = result.stream()
                .filter(r -> r.getUserId().equals("u2"))
                .findFirst().orElse(null);
        assertNotNull(report2);
        assertEquals("u2", report2.getUserId());
        assertEquals("adminUser2", report2.getUsername());
        assertEquals("admin2@example.com", report2.getEmail());
        assertEquals("p2", report2.getParkingId());
        assertEquals("Parking Two", report2.getParking());
        assertEquals(1, report2.getVehicles().size());
        assertEquals(1, report2.getTotalVehicles());
        assertEquals(10.0f, report2.getTotalEarnings());

        VehicleDetailResponse detail2 = report2.getVehicles().get(0);
        assertEquals("v2", detail2.getVehicleId());
        assertEquals("DEF-456", detail2.getPlateNumber());
        assertEquals("Model B", detail2.getModelVehicle());
        assertEquals("15-01-2023-11:00", detail2.getDay());
        assertEquals(10.0f, detail2.getTotalCost());
    }

    @Test
    void getVehiclesOutDetails_NoVehiclesOut_ShouldReturnEmptyList() {
        when(vehicleRepository.findVehiclesWithExitTimeBetween(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        List<VehicleOutDetailResponse> result = vehicleReportService.getVehiclesOutDetails();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getVehiclesOutDetails_VehiclesWithStatusIn_ShouldNotBeIncluded() {
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setId("v1");
        vehicle1.setPlateNumber("ABC-123");
        vehicle1.setModel("Model A");
        vehicle1.setCostPerHour(10.0f);
        vehicle1.setStatus(VehicleStatus.IN);

        Calendar entryCal1 = Calendar.getInstance();
        entryCal1.set(2023, Calendar.JANUARY, 15, 8, 0, 0);
        vehicle1.setEntryTime(entryCal1.getTime());

        Calendar exitCal1 = Calendar.getInstance();
        exitCal1.set(2023, Calendar.JANUARY, 15, 10, 0, 0);
        vehicle1.setExitTime(exitCal1.getTime());

        Parking parking1 = new Parking();
        parking1.setId("p1");
        parking1.setName("Parking One");
        vehicle1.setParking(parking1);

        User admin1 = new User();
        admin1.setId("u1");
        admin1.setUsername("adminUser");
        admin1.setEmail("admin@example.com");
        vehicle1.setAdmin(admin1);

        List<Vehicle> mockVehicles = Collections.singletonList(vehicle1);

        when(vehicleRepository.findVehiclesWithExitTimeBetween(any(Date.class), any(Date.class)))
                .thenReturn(mockVehicles);

        List<VehicleOutDetailResponse> result = vehicleReportService.getVehiclesOutDetails();

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
