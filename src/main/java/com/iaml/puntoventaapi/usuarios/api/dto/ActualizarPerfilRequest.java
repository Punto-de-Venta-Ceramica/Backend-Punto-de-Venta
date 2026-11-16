package com.iaml.puntoventaapi.usuarios.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    String nombre,

    @Size(max = 100, message = "El apellido paterno no puede tener más de 100 caracteres")
    String apellidoPaterno,

    @Size(max = 100, message = "El apellido materno no puede tener más de 100 caracteres")
    String apellidoMaterno,

    @Size(max = 30, message = "El teléfono no puede tener más de 30 caracteres")
    String telefono
) {}

