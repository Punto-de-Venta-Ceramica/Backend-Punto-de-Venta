package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.CredencialRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.RefreshTokenPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.TokenPort;
import org.springframework.stereotype.Service;

@Service
public class RenovarSesionService {

    private final CredencialRepositorioPort credenciales;
    private final RefreshTokenPort refreshTokens;
    private final TokenPort tokens;

    public RenovarSesionService(CredencialRepositorioPort credenciales, RefreshTokenPort refreshTokens,
                                TokenPort tokens) {
        this.credenciales = credenciales;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
    }

    /**
     * Emite un nuevo par de tokens con el rol y permisos vigentes del usuario. El token de
     * renovación también se renueva (vencimiento deslizante): mientras el usuario use la app
     * la sesión no termina; solo cierra sesión voluntariamente o tras un periodo sin uso.
     */
    public Sesion renovar(String refreshToken) {
        RefreshTokenPort.RefreshValido valido = refreshTokens.validar(refreshToken);
        Credencial credencial = credenciales.buscarPorUsuarioId(valido.usuarioId())
            .orElseThrow(CredencialesInvalidasException::new);
        // Si la contraseña cambió después de emitir el token, esa sesión ya no se renueva.
        if (!valido.vigentePara(credencial)) {
            throw new CredencialesInvalidasException();
        }
        return tokens.generar(credencial);
    }
}
