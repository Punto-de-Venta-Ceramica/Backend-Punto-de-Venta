package com.iaml.puntoventaapi.usuarios.api.dto;

import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;

public record PerfilUsuarioResponse(
    Long id,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String correo,
    String telefono,
    String rol
) {
    public static PerfilUsuarioResponse from(UsuarioEntity entity) {
        String rolNombre = entity.getRol() != null ? entity.getRol().getNombre() : "";
        return new PerfilUsuarioResponse(
            entity.getId(),
            entity.getNombre(),
            entity.getApellidoPaterno(),
            entity.getApellidoMaterno(),
            entity.getCorreo(),
            entity.getTelefono(),
            rolNombre
        );
    }
}

