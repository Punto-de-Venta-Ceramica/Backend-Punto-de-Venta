package com.iaml.puntoventaapi.autenticacion.domain.port;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import java.time.Instant;

public interface RefreshTokenPort {

    /** Token de renovación válido: a quién pertenece y cuándo vence. */
    record RefreshValido(Long usuarioId, Instant expiraEn) {
    }

    /**
     * Valida un token de renovación.
     *
     * @throws com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException
     *         si el token es inválido, expiró o no es un token de renovación
     */
    RefreshValido validar(String refreshToken);

    /**
     * Emite un nuevo token de acceso conservando el mismo token de renovación y su vencimiento
     * (la sesión termina siempre a los días fijados desde el inicio de sesión).
     */
    Sesion renovarAcceso(Credencial credencial, String refreshToken, Instant refreshExpiraEn);
}
