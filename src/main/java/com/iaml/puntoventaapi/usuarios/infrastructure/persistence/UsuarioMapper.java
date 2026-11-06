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
        Long sucursalId = entity.getSucursal() != null ? entity.getSucursal().getId() : null;
        return new Usuario(entity.getId(), entity.getRol().getId(), negocioId, sucursalId, entity.getNombre(),
            entity.getApellidoPaterno(), entity.getApellidoMaterno(), entity.getCorreo(),
            entity.getTelefono(), entity.getPasswordHash());
    }

    public static UsuarioEntity toEntity(Usuario usuario, RolEntity rol) {
        return toEntity(usuario, rol, null, null);
    }

    public static UsuarioEntity toEntity(Usuario usuario, RolEntity rol, NegocioEntity negocio, SucursalEntity sucursal) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setRol(rol);
        entity.setNegocio(negocio);
        entity.setSucursal(sucursal);
        entity.setNombre(usuario.getNombre());
        entity.setApellidoPaterno(usuario.getApellidoPaterno());
        entity.setApellidoMaterno(usuario.getApellidoMaterno());
        entity.setCorreo(usuario.getCorreo());
        entity.setTelefono(usuario.getTelefono());
        entity.setPasswordHash(usuario.getPasswordHash());
        return entity;
    }
}
