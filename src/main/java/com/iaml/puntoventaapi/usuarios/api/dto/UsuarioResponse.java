package com.iaml.puntoventaapi.usuarios.api.dto;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;

public record UsuarioResponse(Long id, String nombre, String apellidoPaterno, String apellidoMaterno,
                              String correo, String telefono, Long rolId) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getApellidoPaterno(),
            usuario.getApellidoMaterno(), usuario.getCorreo(), usuario.getTelefono(), usuario.getRolId());
    }
}
