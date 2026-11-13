package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;

/** Un descuento propio de un producto tal como debe quedar. Sin id es un descuento nuevo. */
public record LineaDescuentoCommand(Long id, String nombre, Integer cantidadMinima, Integer cantidadMaxima,
                                    BigDecimal porcentaje, boolean activo) {
}
