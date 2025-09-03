package com.nelumbo.park.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nelumbo.park.configuration.deserializer.StrictFloatDeserializer;
import com.nelumbo.park.configuration.deserializer.StrictIntegerDeserializer;
import com.nelumbo.park.configuration.deserializer.StrictStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParkingUpdateRequest {

    @NotBlank(message = "El nombre del parking no puede estar vacío")
    @Size(min = 5, max = 50, message = "El nombre del parking debe tener entre 5 y 50 caracteres")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String name;

    @NotBlank(message = "La dirección del parking no puede estar vacía")
    @Size(min = 10, max = 100, message = "La dirección del parking debe tener entre 10 y 100 caracteres")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String address;

    @NotNull(message = "El número de capacidad no puede estar vacío")
    @Positive(message = "El número de capacidad debe ser mayor a 0")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer capacity;

    @NotNull(message = "El costo por hora no puede estar vacío")
    @Positive(message = "El costo por hora debe ser mayor a 0")
    @JsonDeserialize(using = StrictFloatDeserializer.class)
    private Float cost_per_hour;

    @NotBlank(message = "El id del administrador no puede estar vacío")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String id_owner;
}
