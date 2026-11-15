package com.iaml.puntoventaapi.autenticacion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "El token de renovación es obligatorio.")
        @Size(max = 4096, message = "El token de renovación no es válido.") String refreshToken) {
}
