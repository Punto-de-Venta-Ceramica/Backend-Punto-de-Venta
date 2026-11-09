package com.iaml.puntoventaapi.autenticacion.domain;

import java.time.Instant;

/**
 * Resultado de un inicio de sesión o de una renovación: token de acceso (corto)
 * y token de renovación (largo).
 */
public record Sesion(String token, Instant expiraEn, String refreshToken, Instant refreshExpiraEn,
                     Long usuarioId, String rol) {
}
