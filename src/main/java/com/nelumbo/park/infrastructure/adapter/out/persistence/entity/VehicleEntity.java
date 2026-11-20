package com.nelumbo.park.infrastructure.adapter.out.persistence.entity;

import com.nelumbo.park.domain.model.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "\"vehicles\"")
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "plate_number", nullable = false)
    private String plateNumber;

    @Column(name = "model_vehicle", nullable = false)
    private String model;

    @Column(name = "entry_time", nullable = false)
    private Date entryTime;

    @Column(name = "exit_time", nullable = true)
    private Date exitTime;

    @Column(name = "cost_per_hour", nullable = false)
    private Float costPerHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.IN;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_parking", nullable = false, referencedColumnName = "id")
    private ParkingEntity parking;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_admin", nullable = false, referencedColumnName = "id")
    private UserEntity admin;
}
