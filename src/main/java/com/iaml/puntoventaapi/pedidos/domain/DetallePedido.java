package com.iaml.puntoventaapi.pedidos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** Aplica el descuento vigente del catálogo únicamente al alcanzar su rango de piezas. */
    public static DetallePedido desdeCatalogo(long productoId, int cantidad, BigDecimal precio,
                                               Integer minimo, Integer maximo, BigDecimal porcentaje) {
        if (minimo == null && maximo == null && porcentaje == null) {
            return new DetallePedido(productoId, cantidad, precio, BigDecimal.ZERO);
        }
        if (minimo == null || minimo < 1 || (maximo != null && maximo < minimo)
                || porcentaje == null || porcentaje.signum() < 0
                || porcentaje.compareTo(new BigDecimal("100")) > 0 || porcentaje.scale() > 2) {
            throw new IllegalArgumentException("El descuento del catálogo no es válido.");
        }
        boolean aplica = cantidad >= minimo && (maximo == null || cantidad <= maximo);
        return new DetallePedido(productoId, cantidad, precio, aplica ? porcentaje : BigDecimal.ZERO);
    }
}
