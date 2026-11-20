package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;

public record ProductoVendidoDto(
    Long idProducto,
    String nombre,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {}

