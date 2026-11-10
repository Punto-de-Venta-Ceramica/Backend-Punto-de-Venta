package com.iaml.puntoventaapi.catalogo.domain;

/** Filtros opcionales del listado de productos; null o texto vacío significa "sin filtro". */
public record FiltroProductos(Long areaId, Long categoriaId, String texto, Long sucursalId) {

    public static final FiltroProductos SIN_FILTROS = new FiltroProductos(null, null, null, null);

    public FiltroProductos {
        texto = texto == null || texto.isBlank() ? null : texto.strip();
    }
}
