package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;
import java.util.List;

/** descuento y existencias son opcionales (null = sin descuento / sucursal por defecto). */
public record CrearProductoCommand(String nombre, BigDecimal precio, Long categoriaId,
                                   DescuentoProductoCommand descuento, List<ExistenciaInicialCommand> existencias) {
}
