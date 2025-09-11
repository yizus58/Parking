package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.TopParkingResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

 class ParkingEarningsCalculatorTest {

    @InjectMocks
    private ParkingEarningsCalculator parkingEarningsCalculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCalculateParkingEarnings_NoVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        List<TopParkingResponse> result = parkingEarningsCalculator.calculateParkingEarnings(vehicles);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateParkingEarnings_NoVehiclesWithExitTime() {
        List<Vehicle> vehicles = new ArrayList<>();
        Parking parking1 = new Parking();
        parking1.setId("p1");
        parking1.setName("Parking 1");

        Vehicle vehicle1 = new Vehicle();
        vehicle1.setParking(parking1);
        vehicle1.setCostPerHour(10.0f);
        vehicle1.setEntryTime(new Date());
        vehicle1.setExitTime(null);
        vehicles.add(vehicle1);

        List<TopParkingResponse> result = parkingEarningsCalculator.calculateParkingEarnings(vehicles);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateParkingEarnings_WithVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();

        Parking parking1 = new Parking();
        parking1.setId("p1");
        parking1.setName("Parking 1");

        Parking parking2 = new Parking();
        parking2.setId("p2");
        parking2.setName("Parking 2");

        Date entry = new Date();
        Date exit = new Date(entry.getTime() + TimeUnit.HOURS.toMillis(2));

        Vehicle vehicle1 = new Vehicle();
        vehicle1.setParking(parking1);
        vehicle1.setCostPerHour(10.0f);
        vehicle1.setEntryTime(entry);
        vehicle1.setExitTime(exit);
        vehicles.add(vehicle1);

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setParking(parking1);
        vehicle2.setCostPerHour(10.0f);
        vehicle2.setEntryTime(entry);
        vehicle2.setExitTime(exit);
        vehicles.add(vehicle2);

        Vehicle vehicle3 = new Vehicle();
        vehicle3.setParking(parking2);
        vehicle3.setCostPerHour(15.0f);
        vehicle3.setEntryTime(entry);
        vehicle3.setExitTime(new Date(entry.getTime() + TimeUnit.HOURS.toMillis(1)));
        vehicles.add(vehicle3);

        List<TopParkingResponse> result = parkingEarningsCalculator.calculateParkingEarnings(vehicles);

        assertEquals(2, result.size());
        assertEquals("p1", result.get(0).getParkingId());
        assertEquals(40.0f, result.get(0).getTotalCost());
        assertEquals("p2", result.get(1).getParkingId());
        assertEquals(15.0f, result.get(1).getTotalCost());
    }

    @Test
    void testCalculateParkingEarnings_Top3Parkings() {
        List<Vehicle> vehicles = new ArrayList<>();
        Date entry = new Date();
        Date exit = new Date(entry.getTime() + TimeUnit.HOURS.toMillis(1));

        for (int i = 1; i <= 4; i++) {
            Parking parking = new Parking();
            parking.setId("p" + i);
            parking.setName("Parking " + i);

            Vehicle vehicle = new Vehicle();
            vehicle.setParking(parking);
            vehicle.setCostPerHour(10.0f * i);
            vehicle.setEntryTime(entry);
            vehicle.setExitTime(exit);
            vehicles.add(vehicle);
        }

        List<TopParkingResponse> result = parkingEarningsCalculator.calculateParkingEarnings(vehicles);

        assertEquals(3, result.size());
        assertEquals("p4", result.get(0).getParkingId());
        assertEquals(40.0f, result.get(0).getTotalCost());
        assertEquals("p3", result.get(1).getParkingId());
        assertEquals(30.0f, result.get(1).getTotalCost());
        assertEquals("p2", result.get(2).getParkingId());
        assertEquals(20.0f, result.get(2).getTotalCost());
    }
}
