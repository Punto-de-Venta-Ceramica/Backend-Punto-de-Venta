package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.catalogo.domain.Producto;
import java.math.BigDecimal;

/** Elemento del listado: solo imagen, nombre y precio. El resto se consulta en el detalle. */
public record ProductoResumenResponse(Long id, String nombre, BigDecimal precio, String imagenUrl) {

    public static ProductoResumenResponse from(Producto producto) {
        return new ProductoResumenResponse(producto.getId(), producto.getNombre(), producto.getPrecio(),
            producto.getImagenUrl());
    }
}
