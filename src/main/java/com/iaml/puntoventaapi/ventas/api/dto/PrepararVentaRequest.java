package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.application.PrepararVentaCommand;
import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.SolicitudVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PrepararVentaRequest(
        @NotNull(message = "Selecciona una sucursal.")
        @Positive(message = "El identificador de la sucursal debe ser positivo.") Long sucursalId,
        @Pattern(regexp = "EFECTIVO|TARJETA", message = "Los métodos admitidos son EFECTIVO y TARJETA.") String metodoPago,
        @NotEmpty(message = "Agrega al menos un producto.")
        @Size(max = SolicitudVenta.MAX_PRODUCTOS, message = "Se admiten máximo 200 productos por venta.")
        List<@NotNull(message = "El producto no puede estar vacío.") @Valid ProductoSolicitadoRequest> productos,
        @DecimalMin(value = "0.00", message = "El monto recibido no puede ser negativo.")
        @Digits(integer = 16, fraction = 2, message = "El monto admite máximo 16 enteros y 2 decimales.")
        BigDecimal montoRecibido) {

    public PrepararVentaCommand toCommand() {
        return new PrepararVentaCommand(sucursalId, metodoPago == null ? null : MetodoPago.valueOf(metodoPago),
            productos.stream().map(ProductoSolicitadoRequest::toDomain).toList(), montoRecibido);
    }
}
