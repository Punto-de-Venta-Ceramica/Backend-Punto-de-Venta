package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.DetalleVenta;
import java.math.BigDecimal;

public record DetalleVentaResponse(Long productoId, String nombreProducto, int cantidad,
                                   BigDecimal precioUnitario, BigDecimal porcentajeDescuento,
                                   BigDecimal importeBruto, BigDecimal descuento, BigDecimal subtotal) {

    public static DetalleVentaResponse from(DetalleVenta detalle) {
        return new DetalleVentaResponse(detalle.productoId(), detalle.nombreProducto(), detalle.cantidad(),
            detalle.precioUnitario(), detalle.porcentajeDescuento(),
            detalle.importeBruto(), detalle.descuento(), detalle.subtotal());
    }
}
