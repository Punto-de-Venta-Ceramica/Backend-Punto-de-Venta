package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(entity.getId(), entity.getRol().getId(), entity.getNombre(),
            entity.getApellidoPaterno(), entity.getApellidoMaterno(), entity.getCorreo(),
            entity.getTelefono(), entity.getPasswordHash());
    }

    public static UsuarioEntity toEntity(Usuario usuario, RolEntity rol) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setRol(rol);
        entity.setNombre(usuario.getNombre());
        entity.setApellidoPaterno(usuario.getApellidoPaterno());
        entity.setApellidoMaterno(usuario.getApellidoMaterno());
        entity.setCorreo(usuario.getCorreo());
        entity.setTelefono(usuario.getTelefono());
        entity.setPasswordHash(usuario.getPasswordHash());
        return entity;
    }
}
