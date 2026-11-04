package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Categoria;

public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static Categoria toDomain(CategoriaEntity entity) {
        return new Categoria(entity.getId(), entity.getArea().getId(), entity.getNombre(), entity.getDescripcion(),
            entity.isActivo(), entity.getFechaEliminacion());
    }

    public static CategoriaEntity toEntity(Categoria categoria, AreaEntity area) {
        CategoriaEntity entity = new CategoriaEntity();
        entity.setArea(area);
        actualizar(categoria, entity);
        return entity;
    }

    public static void actualizar(Categoria categoria, CategoriaEntity entity) {
        entity.setNombre(categoria.getNombre());
        entity.setDescripcion(categoria.getDescripcion());
        entity.setActivo(categoria.isActivo());
        entity.setFechaEliminacion(categoria.getFechaEliminacion());
    }
}
