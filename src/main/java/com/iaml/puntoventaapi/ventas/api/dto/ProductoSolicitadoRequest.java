package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoSolicitadoRequest(
        @NotNull(message = "El producto es obligatorio.")
        @Positive(message = "El identificador del producto debe ser positivo.") Long productoId,
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero.") Integer cantidad,
        Boolean aplicarDescuento,
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero.")
        @Digits(integer = 12, fraction = 2, message = "El precio admite máximo 2 decimales.") BigDecimal precioUnitario,
        @Size(max = ProductoSolicitado.NOMBRE_MAX, message = "El nombre admite máximo 150 caracteres.") String nombre) {

    public ProductoSolicitado toDomain() {
        return new ProductoSolicitado(productoId, cantidad, Boolean.TRUE.equals(aplicarDescuento), precioUnitario, nombre);
    }
}
