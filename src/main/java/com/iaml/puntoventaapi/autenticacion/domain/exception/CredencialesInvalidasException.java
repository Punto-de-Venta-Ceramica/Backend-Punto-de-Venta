package com.iaml.puntoventaapi.autenticacion.domain.exception;

/**
 * Correo inexistente o contraseña incorrecta. El mensaje es el mismo en ambos
 * casos para no revelar qué correos están registrados.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
