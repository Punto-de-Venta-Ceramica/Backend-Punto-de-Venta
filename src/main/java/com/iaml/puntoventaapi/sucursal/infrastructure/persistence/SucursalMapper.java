package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;

public final class SucursalMapper {

    private SucursalMapper() {
    }

    public static Sucursal toDomain(SucursalEntity entity) {
        return new Sucursal(entity.getId(), entity.getNegocio().getId(), entity.getNombre(),
            entity.getDescripcion(), entity.getCiudad(), entity.getCalle(), entity.getCodigoPostal());
    }

    public static SucursalEntity toEntity(Sucursal sucursal, NegocioEntity negocio) {
        var entity = new SucursalEntity();
        entity.setNegocio(negocio);
        entity.setNombre(sucursal.getNombre());
        entity.setDescripcion(sucursal.getDescripcion());
        entity.setCiudad(sucursal.getCiudad());
        entity.setCalle(sucursal.getCalle());
        entity.setCodigoPostal(sucursal.getCodigoPostal());
        return entity;
    }
}

