package com.nelumbo.park.entity;

import com.nelumbo.park.enums.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "\"vehicles\"")
public class Vehicle {

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
    private Parking parking;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_admin", nullable = false, referencedColumnName = "id")
    private User admin;
}
