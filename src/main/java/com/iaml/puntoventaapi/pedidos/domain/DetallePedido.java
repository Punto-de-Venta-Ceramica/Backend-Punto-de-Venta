package com.iaml.puntoventaapi.pedidos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Precio y descuento son una fotografía calculada por el servidor al registrar el pedido. */
public record DetallePedido(long productoId, int cantidad, BigDecimal precioUnitario,
                            BigDecimal porcentajeDescuento) {
    public DetallePedido {
        Objects.requireNonNull(precioUnitario, "El precio es obligatorio.");
        Objects.requireNonNull(porcentajeDescuento, "El descuento es obligatorio.");
        if (productoId <= 0 || cantidad <= 0 || precioUnitario.signum() < 0
                || precioUnitario.scale() > 2 || porcentajeDescuento.scale() > 2
                || porcentajeDescuento.signum() < 0
                || porcentajeDescuento.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El detalle del pedido no es válido.");
        }
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad))
            .multiply(BigDecimal.ONE.subtract(porcentajeDescuento.movePointLeft(2)))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /** Aplica solo el escalón del catálogo en cuyo rango de piezas cae la cantidad; si no cae en ninguno, 0 %. */
    public static DetallePedido desdeCatalogo(long productoId, int cantidad, BigDecimal precio,
                                               List<EscalonDescuento> escalones) {
        BigDecimal porcentaje = EscalonDescuento.paraCantidad(escalones == null ? List.of() : escalones, cantidad)
            .map(EscalonDescuento::porcentaje)
            .orElse(BigDecimal.ZERO);
        return new DetallePedido(productoId, cantidad, precio, porcentaje);
    }
}
