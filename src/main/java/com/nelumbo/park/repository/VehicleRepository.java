package com.nelumbo.park.repository;

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
    
    
    List<Vehicle> findByAdmin(User admin);
    List<Vehicle> findByParking(Parking parking);
    List<Vehicle> findByAdminAndParking(User admin, Parking parking);
    Optional<Vehicle> findByPlateNumberAndStatus(String plateNumber, VehicleStatus status);
    Optional<Vehicle> findByParkingAndPlateNumberAndStatus(Parking parking, String plateNumber, VehicleStatus status);
}
