package com.iaml.puntoventaapi.autenticacion.domain;

import java.time.Instant;

/**
 * Resultado de un inicio de sesión exitoso.
 */
public record Sesion(String token, Instant expiraEn, Long usuarioId, String rol) {
}
