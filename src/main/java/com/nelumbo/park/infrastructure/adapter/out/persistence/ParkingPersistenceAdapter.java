package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.application.port.out.ParkingRepositoryPort;
import com.nelumbo.park.domain.exception.UserNotFoundException;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.ParkingEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.ParkingJpaRepository;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ParkingPersistenceAdapter implements ParkingRepositoryPort {

    private final ParkingJpaRepository parkingRepository;
    private final UserJpaRepository userRepository;

    public ParkingPersistenceAdapter(ParkingJpaRepository parkingRepository, UserJpaRepository userRepository) {
        this.parkingRepository = parkingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Parking> findAll() {
        return parkingRepository.findAll().stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Parking> findById(String id) {
        return parkingRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Parking> findByIdForUpdate(String id) {
        return parkingRepository.findByIdForUpdate(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Parking> findByOwnerId(String ownerId) {
        return parkingRepository.findByOwnerId(ownerId).stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public Parking save(Parking parking) {
        String ownerId = parking.getOwner() != null ? parking.getOwner().getId() : null;
        UserEntity owner = ownerId != null
                ? userRepository.findById(ownerId).orElseThrow(UserNotFoundException::new)
                : null;
        ParkingEntity entity = parking.getId() != null
                ? parkingRepository.findById(parking.getId()).orElseGet(ParkingEntity::new)
                : new ParkingEntity();
        PersistenceMapper.copy(parking, entity, owner);
        return PersistenceMapper.toDomain(parkingRepository.save(entity));
    }

    @Override
    public void deleteById(String id) {
        parkingRepository.findById(id).ifPresent(parkingRepository::delete);
    }
}
