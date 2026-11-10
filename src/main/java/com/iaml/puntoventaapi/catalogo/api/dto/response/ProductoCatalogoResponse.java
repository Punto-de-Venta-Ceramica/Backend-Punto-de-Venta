package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.catalogo.domain.Producto;

/** Producto mínimo para selectores de otros módulos (por ejemplo, asignar descuentos). */
public record ProductoCatalogoResponse(Long id, String nombre, Long categoriaId) {

    public static ProductoCatalogoResponse from(Producto producto) {
        return new ProductoCatalogoResponse(producto.getId(), producto.getNombre(), producto.getCategoriaId());
    }
}
