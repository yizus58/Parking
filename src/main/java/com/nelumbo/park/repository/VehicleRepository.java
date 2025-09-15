package com.nelumbo.park.repository;

import com.nelumbo.park.dto.response.TopPartnerResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
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

    @Query("SELECT v FROM Vehicle v " +
           "WHERE v.status = :status " +
           "GROUP BY v.id, v.plateNumber, v.model, v.entryTime, v.exitTime, v.costPerHour," +
           "v.status, v.parking.id, v.parking.name, v.parking.address, v.parking.capacity, v.parking.costPerHour, v.admin.id " +
           "HAVING COUNT(v.plateNumber) = 1")
    List<Vehicle> findFirstTimeParkedVehicles(VehicleStatus status);

    List<Vehicle> findByAdmin(User admin);
    Optional<Vehicle> findByPlateNumberAndStatus(String plateNumber, VehicleStatus status);

    @Query("SELECT COUNT(v), p.capacity " +
           "FROM Vehicle v JOIN v.parking p " +
           "WHERE p.id = :idParking AND v.status = :status " +
           "GROUP BY p.capacity")
    List<Object[]> findLimitParking(@Param("idParking") String idParking, @Param("status") VehicleStatus status);

    @Query("SELECT v.plateNumber, COUNT(v) as total_visits " +
                  "FROM Vehicle v " +
                  "GROUP BY v.plateNumber " +
                  "ORDER BY COUNT(v) DESC")
    List<Object[]> findTopVehiclesByVisits(Pageable pageable);
    
    @Query("SELECT v.plateNumber, COUNT(v) as total_visits " +
           "FROM Vehicle v JOIN v.parking p " +
           "WHERE p.id = :id " +
           "GROUP BY v.plateNumber " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> findTopVehicleById(String id, Pageable pageable);

    @Query("SELECT new com.nelumbo.park.dto.response.TopPartnerResponse(o.username, COUNT(v.id), p.id) " +
            "FROM Vehicle v JOIN v.parking p JOIN p.owner o " +
            "WHERE o.role = 'SOCIO' AND v.entryTime >= :startOfWeek AND v.entryTime <= :endOfWeek " +
            "GROUP BY o.id, o.username, p.id " +
            "ORDER BY COUNT(v.id) DESC")
    List<TopPartnerResponse> findTopPartnersByWeek(@Param("startOfWeek") Date startOfWeek,
                                                   @Param("endOfWeek") Date endOfWeek,
                                                   Pageable pageable);
    
    @Query("SELECT v FROM Vehicle v WHERE v.exitTime IS NOT NULL AND v.exitTime >= :startDate AND v.exitTime <= :endDate")
    List<Vehicle> findVehiclesWithExitTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
