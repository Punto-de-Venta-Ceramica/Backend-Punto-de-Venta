package com.iaml.puntoventaapi.negocios.domain;

import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;

/** Contenido inmutable de un logo, sin depender de MultipartFile. */
public final class LogoArchivo {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final byte[] contenido;

    public LogoArchivo(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new LogoInvalidoException("El logo no puede estar vacío");
        }
        if (contenido.length > MAX_BYTES) {
            throw new LogoInvalidoException("El logo no debe superar 2 MB");
        }
        this.contenido = contenido.clone();
    }

    public byte[] getContenido() {
        return contenido.clone();
    }
}
