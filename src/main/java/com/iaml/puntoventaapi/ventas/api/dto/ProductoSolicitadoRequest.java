package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductoSolicitadoRequest(
        @NotNull(message = "El producto es obligatorio.")
        @Positive(message = "El identificador del producto debe ser positivo.") Long productoId,
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero.") Integer cantidad,
        Boolean aplicarDescuento) {

    public ProductoSolicitado toDomain() {
        return new ProductoSolicitado(productoId, cantidad, Boolean.TRUE.equals(aplicarDescuento));
    }
}
