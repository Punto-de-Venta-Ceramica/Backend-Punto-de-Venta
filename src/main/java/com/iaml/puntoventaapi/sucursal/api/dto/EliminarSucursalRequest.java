package com.iaml.puntoventaapi.sucursal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EliminarSucursalRequest(
        @NotNull(message = "El id de la sucursal es obligatorio.")
        @Positive(message = "El id de la sucursal debe ser positivo.") Long id) {
}

