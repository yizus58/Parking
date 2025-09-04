package com.nelumbo.park.repository;

import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    
    @Query("SELECT v FROM Vehicle v WHERE v.plateNumber = :plateNumber AND v.status = :status")
    List<Vehicle> findByPlateNumberAndStatus(String plateNumber, String status);

    @Query("SELECT v FROM Vehicle v " +
           "WHERE v.status = :status " +
           "GROUP BY v.id, v.plateNumber, v.model, v.entryTime, v.exitTime, v.costPerHour, v.status, v.parking.id, v.parking.name, v.parking.address, v.parking.capacity, v.parking.costPerHour, v.admin.id " +
           "HAVING COUNT(v.plateNumber) = 1")
    List<Vehicle> findFirstTimeParkedVehicles(VehicleStatus status);

    List<Vehicle> findByAdmin(User admin);
    List<Vehicle> findByParking(Parking parking);
    List<Vehicle> findByAdminAndParking(User admin, Parking parking);
    Optional<Vehicle> findByPlateNumberAndStatus(String plateNumber, VehicleStatus status);
    Optional<Vehicle> findByParkingAndPlateNumberAndStatus(Parking parking, String plateNumber, VehicleStatus status);

    @Query(value = "SELECT v.plateNumber, COUNT(*) as total_visits " +
                  "FROM Vehicle v " +
                  "GROUP BY v.plateNumber " +
                  "ORDER BY COUNT(*) DESC " +
                  "LIMIT 10")
    List<Object[]> findTopVehiclesByVisits();
    
    @Query("SELECT v.plateNumber, COUNT(v) as total_visits " +
           "FROM Vehicle v " + 
           "INNER JOIN Parking p ON v.parking = p " +
           "WHERE p.id = :id " +
           "GROUP BY v.plateNumber " +
           "ORDER BY COUNT(v) DESC " +
           "LIMIT 10")
    List<Object[]> findTopVehicleById(String id);
}
