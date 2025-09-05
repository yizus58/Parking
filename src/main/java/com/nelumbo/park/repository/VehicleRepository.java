package com.nelumbo.park.repository;

import com.nelumbo.park.dto.response.TopParkingResponse;
import com.nelumbo.park.dto.response.TopPartnerResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Date;
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

    @Query("SELECT new com.nelumbo.park.dto.response.TopPartnerResponse(p.owner.username, COUNT(v.id)) " +
            "FROM Vehicle v JOIN v.parking p " +
            "WHERE p.owner.role = 'SOCIO' AND v.entryTime >= :startOfWeek AND v.entryTime <= :endOfWeek " +
            "GROUP BY p.owner.id, p.owner.username " +
            "ORDER BY COUNT(v.id) DESC")
    List<TopPartnerResponse> findTopPartnersByWeek(@Param("startOfWeek") Date startOfWeek,
                                                   @Param("endOfWeek") Date endOfWeek,
                                                   Pageable pageable);
        @Query("SELECT v FROM Vehicle v WHERE v.exitTime IS NOT NULL AND v.exitTime >= :startDate AND v.exitTime <= :endDate")
        List<Vehicle> findVehiclesWithExitTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
