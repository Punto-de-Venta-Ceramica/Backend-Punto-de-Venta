package com.iaml.puntoventaapi.ventas.domain;

import java.math.BigDecimal;

/** Precio vigente y descuento por volumen resueltos por el catálogo para la cantidad solicitada. */
public record ProductoCotizado(Long productoId, String nombre, BigDecimal precioUnitario,
                               BigDecimal porcentajeDescuento) {
}
