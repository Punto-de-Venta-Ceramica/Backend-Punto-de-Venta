package com.iaml.puntoventaapi.autenticacion.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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

    /**
     * Huella corta del hash de la contraseña. Viaja en el token de renovación: si la contraseña cambia,
     * la huella deja de coincidir y las sesiones abiertas ya no pueden renovarse.
     */
    public String huellaPassword() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
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
