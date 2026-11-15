package com.iaml.puntoventaapi.autenticacion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarCodigoRequest(
    @Schema(description = "Correo electrónico del usuario", example = "usuario@ejemplo.com")
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    String correo
) {
    public SolicitarCodigoRequest() {
        this("");
    }
}

