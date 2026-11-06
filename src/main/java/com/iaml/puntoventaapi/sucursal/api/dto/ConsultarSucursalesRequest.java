package com.iaml.puntoventaapi.sucursal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsultarSucursalesRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.") Long negocioId) {
}

