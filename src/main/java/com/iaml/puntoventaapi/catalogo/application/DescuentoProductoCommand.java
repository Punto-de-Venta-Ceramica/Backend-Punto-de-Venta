package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;

public record DescuentoProductoCommand(Integer cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) {
}
