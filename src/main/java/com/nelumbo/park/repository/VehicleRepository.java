package com.nelumbo.park.repository;

import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByPlateNumber(String plateNumber);
    List<Vehicle> findByAdmin(User admin);
    List<Vehicle> findByParking(Parking parking);
    List<Vehicle> findByAdminAndParking(User admin, Parking parking);
}
