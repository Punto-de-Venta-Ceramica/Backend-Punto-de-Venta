package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.domain.Permiso;
import com.iaml.puntoventaapi.autenticacion.domain.Rol;
import java.util.stream.Collectors;

public final class RolMapper {

    private RolMapper() {
    }

    public static Rol toDomain(RolEntity entity) {
        return new Rol(entity.getId(), entity.getNombre(), entity.getPermisos().stream()
            .map(p -> new Permiso(p.getId(), p.getNombre()))
            .collect(Collectors.toSet()));
    }
}
