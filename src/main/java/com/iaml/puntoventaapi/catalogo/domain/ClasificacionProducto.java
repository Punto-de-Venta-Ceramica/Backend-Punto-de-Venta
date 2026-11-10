package com.iaml.puntoventaapi.catalogo.domain;

/** Categoría y área de un producto; solo se resuelve al consultar su detalle. */
public record ClasificacionProducto(Long areaId, String areaNombre, Long categoriaId, String categoriaNombre) {
}
