package com.iaml.puntoventaapi.autenticacion.domain;

import java.time.Instant;

/**
 * Código temporal de 6 dígitos para recuperar la contraseña.
 */
public final class CodigoRecuperacion {

    private final String correo;
    private final String codigo;
    private final Instant expiraEn;

    public CodigoRecuperacion(String correo, String codigo, Instant expiraEn) {
        this.correo = correo;
        this.codigo = codigo;
        this.expiraEn = expiraEn;
    }

    public CodigoRecuperacion() {
        this("", "", Instant.EPOCH);
    }

    public String getCorreo() { return correo; }
    public String getCodigo() { return codigo; }
    public Instant getExpiraEn() { return expiraEn; }

    public boolean estaVigente() {
        return Instant.now().isBefore(expiraEn);
    }

    public boolean coincide(String candidato) {
        return codigo != null && codigo.equals(candidato);
    }
}

