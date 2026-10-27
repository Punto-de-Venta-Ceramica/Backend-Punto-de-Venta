package com.iaml.puntoventaapi.negocios.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;

public final class NegocioMapper {

    private NegocioMapper() {
    }

    public static Negocio toDomain(NegocioEntity entity) {
        return new Negocio(entity.getId(), entity.getNombre(), entity.getLogo(),
            entity.getFechaCreacion(), entity.getFechaActualizacion(), entity.getFechaEliminacion(),
            entity.isActivo(), entity.getVersion());
    }

    public static NegocioEntity toEntity(Negocio negocio, UsuarioEntity usuario) {
        var entity = new NegocioEntity();
        actualizar(negocio, entity);
        entity.getUsuarios().add(usuario);
        return entity;
    }

    static void actualizar(Negocio negocio, NegocioEntity entity) {
        entity.setNombre(negocio.getNombre());
        entity.setLogo(negocio.getLogo());
        entity.setActivo(negocio.isActivo());
        entity.setFechaEliminacion(negocio.getFechaEliminacion());
    }
}
