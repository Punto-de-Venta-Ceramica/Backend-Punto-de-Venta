package com.iaml.puntoventaapi.catalogo.application;

import java.math.BigDecimal;

/** descuento null significa que el producto queda sin descuento. */
public record EditarProductoCommand(Long id, String nombre, BigDecimal precio, Long categoriaId,
                                    DescuentoProductoCommand descuento) {
}
