package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;

public final class SucursalMapper {

    private SucursalMapper() {
    }

    public static Sucursal toDomain(com.iaml.puntoventaapi.negocios.domain.Sucursal entity) {
        return new Sucursal(entity.getId(), entity.getNegocio().getId(), entity.getNombre(),
            entity.getDescripcion(), entity.getCiudad(), entity.getCalle(), entity.getCodigoPostal());
    }

    public static com.iaml.puntoventaapi.negocios.domain.Sucursal toEntity(Sucursal sucursal, Negocio negocio) {
        var entity = new com.iaml.puntoventaapi.negocios.domain.Sucursal();
        entity.setNegocio(negocio);
        entity.setNombre(sucursal.getNombre());
        entity.setDescripcion(sucursal.getDescripcion());
        entity.setCiudad(sucursal.getCiudad());
        entity.setCalle(sucursal.getCalle());
        entity.setCodigoPostal(sucursal.getCodigoPostal());
        return entity;
    }
}

