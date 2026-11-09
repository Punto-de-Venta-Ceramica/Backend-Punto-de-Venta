package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.CredencialRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.RefreshTokenPort;
import org.springframework.stereotype.Service;

@Service
public class RenovarSesionService {

    private final CredencialRepositorioPort credenciales;
    private final RefreshTokenPort refreshTokens;

    public RenovarSesionService(CredencialRepositorioPort credenciales, RefreshTokenPort refreshTokens) {
        this.credenciales = credenciales;
        this.refreshTokens = refreshTokens;
    }

    /** Emite un nuevo token de acceso con el rol y permisos vigentes del usuario. */
    public Sesion renovar(String refreshToken) {
        RefreshTokenPort.RefreshValido valido = refreshTokens.validar(refreshToken);
        Credencial credencial = credenciales.buscarPorUsuarioId(valido.usuarioId())
            .orElseThrow(CredencialesInvalidasException::new);
        return refreshTokens.renovarAcceso(credencial, refreshToken, valido.expiraEn());
    }
}
