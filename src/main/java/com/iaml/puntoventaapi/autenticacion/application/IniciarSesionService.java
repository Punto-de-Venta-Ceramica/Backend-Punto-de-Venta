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
    private final ControlIntentos intentos = new ControlIntentos();

    // Hash BCrypt con formato válido (costo 10) que no corresponde a ninguna cuenta.
    private static final String HASH_SENUELO = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    public IniciarSesionService(CredencialRepositorioPort credenciales, PasswordHasherPort hasher, TokenPort tokens) {
        this.credenciales = credenciales;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    public Sesion iniciarSesion(IniciarSesionCommand command) {
        intentos.verificar(command.correo());
        try {
            Credencial credencial = credenciales.buscarPorCorreo(command.correo())
                .orElseThrow(() -> correoInexistente(command.password()));
            credencial.verificarPassword(command.password(), hasher);
            intentos.limpiar(command.correo());
            return tokens.generar(credencial);
        } catch (CredencialesInvalidasException ex) {
            intentos.registrarFallo(command.correo());
            throw ex;
        }
    }

    /** Hace la misma verificación que con un correo real: el tiempo de respuesta no delata si existe. */
    private CredencialesInvalidasException correoInexistente(String password) {
        hasher.coincide(password == null ? "" : password, HASH_SENUELO);
        return new CredencialesInvalidasException();
    }
}
