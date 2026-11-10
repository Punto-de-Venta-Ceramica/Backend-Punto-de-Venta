package com.iaml.puntoventaapi.ventas.domain;

import java.math.BigDecimal;

public record ProductoDisponible(Long productoId, String nombre, BigDecimal precioUnitario, int existencia) {
}
