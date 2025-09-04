package com.nelumbo.park.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nelumbo.park.configuration.deserializer.StrictFloatDeserializer;
import com.nelumbo.park.configuration.deserializer.StrictIntegerDeserializer;
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
public class VehicleUpdateRequest {

    @NotBlank(message = "La placa del vehículo es obligatoria")
    @Size(max = 10, message = "La placa no debe exceder 10 caracteres")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String plate_number;

    @NotBlank(message = "El modelo del vehículo es obligatorio")
    @Size(max = 50, message = "El modelo no debe exceder 50 caracteres")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String model_vehicle;

    @NotNull(message = "La hora de entrada es obligatoria")
    @JsonDeserialize(using = StrictFloatDeserializer.class)
    private Date entry_time;

    @JsonDeserialize(using = StrictFloatDeserializer.class)
    private Date exit_time;

    @NotBlank(message = "El ID del parking es obligatorio")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String id_parking;

    @NotBlank(message = "El ID del administrador es obligatorio")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String id_admin;

    @NotNull(message = "El costo por hora es obligatorio")
    @Positive(message = "El costo por hora debe ser mayor a 0")
    @JsonDeserialize(using = StrictFloatDeserializer.class)
    private Float cost_per_hour;

    @NotNull(message = "El estado del vehículo es obligatorio")
    private VehicleStatus status;
}
