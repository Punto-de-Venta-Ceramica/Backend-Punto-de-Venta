package com.iaml.puntoventaapi.inventario.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Asigna un producto existente a una sucursal con una existencia inicial (0 si se omite). */
public record AsignarProductoRequest(
        @NotNull(message = "El producto es obligatorio.") Long productoId,
        @NotNull(message = "La sucursal es obligatoria.") Long sucursalId,
        @Min(value = 0, message = "La existencia inicial no puede ser negativa.")
        @Max(value = 1_000_000, message = "La existencia inicial no puede ser mayor a 1,000,000.") Integer existencia) {

    public int existenciaInicial() {
        return existencia == null ? 0 : existencia;
    }
}
