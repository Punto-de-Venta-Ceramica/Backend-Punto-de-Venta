package com.iaml.puntoventaapi.autenticacion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
    @Schema(description = "Correo del usuario", example = "usuario@ejemplo.com")
    @NotBlank @Email
    String correo,

    @Schema(description = "Código de 6 dígitos recibido por email", example = "483921")
    @NotBlank @Pattern(regexp = "\\d{6}", message = "El código debe tener exactamente 6 dígitos")
    String codigo,

    @Schema(description = "Nueva contraseña (mínimo 8 caracteres)", example = "NuevaPass456")
    @NotBlank @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    String nuevaPassword,

    @Schema(description = "Confirmación de la nueva contraseña", example = "NuevaPass456")
    @NotBlank @Size(min = 8)
    String confirmarPassword
) {
    public RestablecerPasswordRequest() {
        this("", "", "", "");
    }
}

