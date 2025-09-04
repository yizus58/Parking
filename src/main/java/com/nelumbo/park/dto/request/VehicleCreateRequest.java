package com.nelumbo.park.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nelumbo.park.configuration.deserializer.StrictDateDeserializer;
import com.nelumbo.park.configuration.deserializer.StrictFloatDeserializer;
import com.nelumbo.park.configuration.deserializer.StrictStringDeserializer;
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
public class VehicleCreateRequest {

    @NotBlank(message = "La placa del vehículo es obligatoria")
    @Size(max = 10, message = "La placa no debe exceder 10 caracteres")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String plate_number;

    @NotBlank(message = "El modelo del vehículo es obligatorio")
    @Size(max = 50, message = "El modelo no debe exceder 50 caracteres")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String model_vehicle;

    @JsonDeserialize(using = StrictDateDeserializer.class)
    private Date entry_time;

    @JsonDeserialize(using = StrictDateDeserializer.class)
    private Date exit_time;

    @NotBlank(message = "El ID del parking es obligatorio")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String id_parking;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String id_admin;

    @Positive(message = "El costo por hora debe ser mayor a 0")
    @JsonDeserialize(using = StrictFloatDeserializer.class)
    private Float cost_per_hour;

    private VehicleStatus status = VehicleStatus.IN;
}
