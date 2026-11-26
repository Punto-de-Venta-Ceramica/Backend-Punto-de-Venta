package com.iaml.puntoventaapi.ventas.domain;

import java.math.BigDecimal;

/**
 * Precio vigente y descuento por volumen resueltos por el catálogo para la cantidad solicitada.
 * precioVariable: producto "Artículo artesanal", cuyo precio y nombre se capturan en la venta.
 */
public record ProductoCotizado(Long productoId, String nombre, BigDecimal precioUnitario,
                               BigDecimal porcentajeDescuento, boolean precioVariable) {

    public ProductoCotizado(Long productoId, String nombre, BigDecimal precioUnitario, BigDecimal porcentajeDescuento) {
        this(productoId, nombre, precioUnitario, porcentajeDescuento, false);
    }
}
