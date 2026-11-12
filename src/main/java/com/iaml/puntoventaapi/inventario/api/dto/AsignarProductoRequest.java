package com.iaml.puntoventaapi.inventario.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Asigna un producto existente a una sucursal. Sin existencia (null): 0 en una asignación nueva, o la que
 * tenía si el producto se había quitado de esa sucursal.
 */
public record AsignarProductoRequest(
        @NotNull(message = "El producto es obligatorio.") Long productoId,
        @NotNull(message = "La sucursal es obligatoria.") Long sucursalId,
        @Min(value = 0, message = "La existencia inicial no puede ser negativa.")
        @Max(value = 1_000_000, message = "La existencia inicial no puede ser mayor a 1,000,000.") Integer existencia) {
}
