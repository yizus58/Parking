package com.nelumbo.park.repository;

import com.nelumbo.park.entity.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {
    Parking findById(String id);
    boolean existsById(String id);

    Parking findByName(String name);
}
