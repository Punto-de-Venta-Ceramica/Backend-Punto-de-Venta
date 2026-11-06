package com.iaml.puntoventaapi.usuarios;

/**
 * Datos que el módulo de usuarios expone para autenticar a un usuario.
 */
public record CredencialesUsuario(Long usuarioId, String correo, String passwordHash, Long rolId) {
}
