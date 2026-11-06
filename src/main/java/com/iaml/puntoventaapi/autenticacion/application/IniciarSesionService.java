package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.CredencialRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.PasswordHasherPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.TokenPort;
import org.springframework.stereotype.Service;

@Service
public class IniciarSesionService {

    private final CredencialRepositorioPort credenciales;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;

    public IniciarSesionService(CredencialRepositorioPort credenciales, PasswordHasherPort hasher, TokenPort tokens) {
        this.credenciales = credenciales;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    public Sesion iniciarSesion(IniciarSesionCommand command) {
        Credencial credencial = credenciales.buscarPorCorreo(command.correo())
            .orElseThrow(CredencialesInvalidasException::new);
        credencial.verificarPassword(command.password(), hasher);
        return tokens.generar(credencial);
    }
}
