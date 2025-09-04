package com.nelumbo.park.dto.request;


import com.nelumbo.park.enums.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleUpdateRequest {

    @NotBlank(message = "La placa del vehículo es obligatoria")
    @Size(max = 10, message = "La placa no debe exceder 10 caracteres")
    private String plateNumber;

    @NotBlank(message = "El modelo del vehículo es obligatorio")
    @Size(max = 50, message = "El modelo no debe exceder 50 caracteres")
    private String modelVehicle;

    private Date entryTime;

    private Date exitTime;

    @NotBlank(message = "El ID del parking es obligatorio")
    private String idParking;

    private String idAdmin;

    private Float costPerHour;

    private VehicleStatus status;
}
