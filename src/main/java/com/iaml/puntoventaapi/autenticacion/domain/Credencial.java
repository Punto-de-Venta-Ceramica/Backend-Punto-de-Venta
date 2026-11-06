package com.iaml.puntoventaapi.autenticacion.domain;

import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.PasswordHasherPort;

/**
 * Datos con los que un usuario se identifica: correo, hash de su contraseña y rol.
 */
public final class Credencial {

    private final Long usuarioId;
    private final String correo;
    private final String passwordHash;
    private final Rol rol;

    public Credencial(Long usuarioId, String correo, String passwordHash, Rol rol) {
        if (usuarioId == null || correo == null || passwordHash == null || rol == null) {
            throw new IllegalArgumentException("Credencial incompleta");
        }
        this.usuarioId = usuarioId;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    /** Lanza CredencialesInvalidasException si la contraseña no corresponde al hash. */
    public void verificarPassword(String passwordPlano, PasswordHasherPort hasher) {
        if (passwordPlano == null || !hasher.coincide(passwordPlano, passwordHash)) {
            throw new CredencialesInvalidasException();
        }
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getCorreo() {
        return correo;
    }

    public Rol getRol() {
        return rol;
    }
}
