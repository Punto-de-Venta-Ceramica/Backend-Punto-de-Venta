package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toDomain(UsuarioEntity entity) {
        Long negocioId = entity.getNegocio() != null ? entity.getNegocio().getId() : null;
        java.util.List<Long> sucursalIds = entity.getSucursales().stream().map(SucursalEntity::getId).sorted().toList();
        return new Usuario(entity.getId(), entity.getRol().getId(), negocioId, sucursalIds, entity.getNombre(),
            entity.getApellidoPaterno(), entity.getApellidoMaterno(), entity.getCorreo(),
            entity.getTelefono(), entity.getPasswordHash());
    }

    public static UsuarioEntity toEntity(Usuario usuario, RolEntity rol) {
        return toEntity(usuario, rol, null, java.util.List.of());
    }

    public static UsuarioEntity toEntity(Usuario usuario, RolEntity rol, NegocioEntity negocio, java.util.Collection<SucursalEntity> sucursales) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setRol(rol);
        entity.setNegocio(negocio);
        entity.setSucursales(new java.util.LinkedHashSet<>(sucursales));
        entity.setNombre(usuario.getNombre());
        entity.setApellidoPaterno(usuario.getApellidoPaterno());
        entity.setApellidoMaterno(usuario.getApellidoMaterno());
        entity.setCorreo(usuario.getCorreo());
        entity.setTelefono(usuario.getTelefono());
        entity.setPasswordHash(usuario.getPasswordHash());
        return entity;
    }
}
