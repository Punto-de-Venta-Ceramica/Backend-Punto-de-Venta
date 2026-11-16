package com.iaml.puntoventaapi.autenticacion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
    @Schema(description = "Contraseña actual del usuario", example = "MiPassword123")
    @NotBlank(message = "La contraseña actual es obligatoria")
    String passwordActual,

    @Schema(description = "Nueva contraseña (mínimo 8 caracteres)", example = "NuevaPass456")
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    String nuevaPassword,

    @Schema(description = "Confirmación de la nueva contraseña", example = "NuevaPass456")
    @NotBlank(message = "La confirmación de la nueva contraseña es obligatoria")
    @Size(min = 8, message = "La confirmación debe tener al menos 8 caracteres")
    String confirmarPassword
) {}

