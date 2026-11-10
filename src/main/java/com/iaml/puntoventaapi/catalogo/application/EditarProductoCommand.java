package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;

/** descuento e imagen null significan que no se proporciona ese dato opcional. */
public record EditarProductoCommand(Long id, String nombre, BigDecimal precio, Long categoriaId,
                                    String imagenUrl, String imagenPublicId,
                                    DescuentoProductoCommand descuento) {

    public EditarProductoCommand(Long id, String nombre, BigDecimal precio, Long categoriaId,
                                 DescuentoProductoCommand descuento) {
        this(id, nombre, precio, categoriaId, null, null, descuento);
    }
}
