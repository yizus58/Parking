package com.nelumbo.park.infrastructure.adapter.out.persistence.repository;

import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.VehicleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface VehicleJpaRepository extends JpaRepository<VehicleEntity, String> {

    @Query("SELECT v FROM VehicleEntity v " +
           "WHERE v.status = :status " +
           "GROUP BY v.id, v.plateNumber, v.model, v.entryTime, v.exitTime, v.costPerHour," +
           "v.status, v.parking.id, v.parking.name, v.parking.address, v.parking.capacity, v.parking.costPerHour, v.admin.id " +
           "HAVING COUNT(v.plateNumber) = 1")
    List<VehicleEntity> findFirstTimeParkedVehicles(@Param("status") VehicleStatus status);

    List<VehicleEntity> findByAdminId(String adminId);

    List<VehicleEntity> findByParkingId(String parkingId);

    Optional<VehicleEntity> findFirstByPlateNumberAndStatus(String plateNumber, VehicleStatus status);

    long countByParkingIdAndStatus(String parkingId, VehicleStatus status);

    @Query("SELECT v.plateNumber, COUNT(v) as total_visits " +
           "FROM VehicleEntity v " +
           "GROUP BY v.plateNumber " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> findTopVehiclesByVisits(Pageable pageable);

    @Query("SELECT v.plateNumber, COUNT(v) as total_visits " +
           "FROM VehicleEntity v JOIN v.parking p " +
           "WHERE p.id = :id " +
           "GROUP BY v.plateNumber " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> findTopVehiclesByParking(@Param("id") String id, Pageable pageable);

    @Query("SELECT new com.nelumbo.park.domain.model.stats.TopPartner(o.username, COUNT(v.id), p.id) " +
           "FROM VehicleEntity v JOIN v.parking p JOIN p.owner o " +
           "WHERE o.role = 'SOCIO' AND v.entryTime >= :from AND v.entryTime <= :to " +
           "GROUP BY o.id, o.username, p.id " +
           "ORDER BY COUNT(v.id) DESC")
    List<TopPartner> findTopPartners(@Param("from") Date from, @Param("to") Date to, Pageable pageable);

    @Query("SELECT v FROM VehicleEntity v WHERE v.exitTime IS NOT NULL AND v.exitTime >= :from AND v.exitTime <= :to")
    List<VehicleEntity> findExitedBetween(@Param("from") Date from, @Param("to") Date to);
}
