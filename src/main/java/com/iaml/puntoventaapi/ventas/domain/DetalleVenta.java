package com.iaml.puntoventaapi.ventas.domain;

import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Importes de una línea calculados con la misma fórmula de detalle_venta.subtotal.
 * precioVariable: línea del producto "Artículo artesanal"; no tiene inventario que descontar.
 */
public record DetalleVenta(Long productoId, String nombreProducto, int cantidad,
                           BigDecimal precioUnitario, BigDecimal porcentajeDescuento, boolean precioVariable) {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal IMPORTE_MAXIMO = new BigDecimal("9999999999999999.99");

    public DetalleVenta {
        if (productoId == null || productoId <= 0 || nombreProducto == null || nombreProducto.isBlank()
                || cantidad <= 0 || precioUnitario == null || precioUnitario.signum() < 0
                || precioUnitario.scale() > 2 || precioUnitario.precision() - precioUnitario.scale() > 12
                || porcentajeDescuento == null || porcentajeDescuento.signum() < 0
                || porcentajeDescuento.compareTo(CIEN) > 0 || porcentajeDescuento.scale() > 2) {
            throw new DatosVentaInvalidosException(Map.of("productos", "Los datos de algún producto no son válidos."));
        }
        precioUnitario = precioUnitario.setScale(2);
        porcentajeDescuento = porcentajeDescuento.setScale(2);
        if (precioUnitario.multiply(BigDecimal.valueOf(cantidad)).compareTo(IMPORTE_MAXIMO) > 0) {
            throw new DatosVentaInvalidosException(Map.of("productos", "El importe de algún producto supera el máximo permitido."));
        }
    }

    public DetalleVenta(Long productoId, String nombreProducto, int cantidad,
                        BigDecimal precioUnitario, BigDecimal porcentajeDescuento) {
        this(productoId, nombreProducto, cantidad, precioUnitario, porcentajeDescuento, false);
    }

    public BigDecimal importeBruto() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public BigDecimal subtotal() {
        return importeBruto().multiply(BigDecimal.ONE.subtract(porcentajeDescuento.divide(CIEN)))
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal descuento() {
        return importeBruto().subtract(subtotal());
    }
}
