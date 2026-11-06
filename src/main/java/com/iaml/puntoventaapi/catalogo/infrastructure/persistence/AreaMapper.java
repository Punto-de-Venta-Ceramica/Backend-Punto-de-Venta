package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;

public final class AreaMapper {

    private AreaMapper() {
    }

    public static Area toDomain(AreaEntity entity) {
        return new Area(entity.getId(), entity.getNegocio().getId(), entity.getNombre(), entity.getDescripcion(),
            entity.isActivo(), entity.getFechaEliminacion());
    }

    public static AreaEntity toEntity(Area area, NegocioEntity negocio) {
        AreaEntity entity = new AreaEntity();
        entity.setNegocio(negocio);
        actualizar(area, entity);
        return entity;
    }

    public static void actualizar(Area area, AreaEntity entity) {
        entity.setNombre(area.getNombre());
        entity.setDescripcion(area.getDescripcion());
        entity.setActivo(area.isActivo());
        entity.setFechaEliminacion(area.getFechaEliminacion());
    }
}
