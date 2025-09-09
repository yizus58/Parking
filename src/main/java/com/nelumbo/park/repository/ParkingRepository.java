package com.nelumbo.park.repository;

import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {
    Parking findById(String id);

    @Query("SELECT p FROM Parking p WHERE p.owner = :owner")
    List<Parking> findByOwner(User owner);

    Parking findByIdAndOwner(String id, User owner);
}
