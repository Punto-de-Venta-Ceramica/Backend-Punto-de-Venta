package com.iaml.puntoventaapi.inventario.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Entrada de piezas: solo suma, nunca resta. */
public record AgregarExistenciaRequest(
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero.")
        @Max(value = 1_000_000, message = "La cantidad no puede ser mayor a 1,000,000.") Integer cantidad) {
}
