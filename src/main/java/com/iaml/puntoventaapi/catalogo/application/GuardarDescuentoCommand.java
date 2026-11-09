package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;

public record GuardarDescuentoCommand(String nombre, Integer cantidadMinima, Integer cantidadMaxima,
                                      BigDecimal porcentaje) {
}
