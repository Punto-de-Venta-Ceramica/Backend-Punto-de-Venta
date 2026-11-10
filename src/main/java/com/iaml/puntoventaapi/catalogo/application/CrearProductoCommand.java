package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;
import java.util.List;

/** descuento, imagen y existencias son opcionales (null = sin descuento / sin imagen / sucursal por defecto). */
public record CrearProductoCommand(String nombre, BigDecimal precio, Long categoriaId,
                                   String imagenUrl, String imagenPublicId,
                                   DescuentoProductoCommand descuento, List<ExistenciaInicialCommand> existencias) {

    public CrearProductoCommand(String nombre, BigDecimal precio, Long categoriaId,
                                DescuentoProductoCommand descuento, List<ExistenciaInicialCommand> existencias) {
        this(nombre, precio, categoriaId, null, null, descuento, existencias);
    }
}
