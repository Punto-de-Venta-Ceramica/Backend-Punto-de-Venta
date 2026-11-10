package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.catalogo.domain.Categoria;

public record CategoriaResponse(Long id, Long areaId, String nombre, String descripcion) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getAreaId(),
            categoria.getNombre(), categoria.getDescripcion());
    }
}
