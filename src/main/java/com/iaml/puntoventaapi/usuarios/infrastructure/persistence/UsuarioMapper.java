package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(entity.getId(), entity.getRol().getId(), entity.getNombre(),
            entity.getApellidoPaterno(), entity.getApellidoMaterno(), entity.getCorreo(),
            entity.getTelefono(), entity.getPasswordHash());
    }
}
