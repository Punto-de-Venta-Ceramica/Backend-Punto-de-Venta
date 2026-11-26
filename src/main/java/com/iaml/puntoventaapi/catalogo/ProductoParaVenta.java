package com.iaml.puntoventaapi.catalogo;

import java.math.BigDecimal;

/** Datos de solo lectura del catálogo para preparar una venta. */
public record ProductoParaVenta(Long productoId, String nombre, BigDecimal precioUnitario,
                                BigDecimal porcentajeDescuento, boolean precioVariable) {

    public ProductoParaVenta(Long productoId, String nombre, BigDecimal precioUnitario, BigDecimal porcentajeDescuento) {
        this(productoId, nombre, precioUnitario, porcentajeDescuento, false);
    }
}
