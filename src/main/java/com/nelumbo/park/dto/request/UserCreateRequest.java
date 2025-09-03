package com.nelumbo.park.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nelumbo.park.configuration.deserializer.StrictStringDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "El username es obligatorio")
    @Size(max = 50, message = "El username no debe exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, números, puntos, guiones y guiones bajos")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String email;

    @NotBlank(message = "El password es obligatorio")
    @Size(min = 6, message = "El password debe tener al menos 6 caracteres")
    @Pattern(regexp = "^.+$", message = "El password debe ser una cadena de texto válida")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String password;

}
