package com.iaml.puntoventaapi.autenticacion.api.dto;

import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEn, String refreshToken,
                            Instant refreshExpiraEn, Long usuarioId, String rol) {

    public static LoginResponse from(Sesion sesion) {
        return new LoginResponse(sesion.token(), "Bearer", sesion.expiraEn(), sesion.refreshToken(),
            sesion.refreshExpiraEn(), sesion.usuarioId(), sesion.rol());
    }
}
