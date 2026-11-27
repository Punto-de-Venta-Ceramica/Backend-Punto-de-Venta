package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;

/** {@code metodoPago} es EFECTIVO o TARJETA en ventas directas y null en productos de pedidos. */
public record ProductoVendidoDto(
    Long idProducto,
    String nombre,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal,
    String metodoPago
) {}
