package com.iaml.puntoventaapi.shared.security;

import org.springframework.security.core.AuthenticationException;

public class UsuarioNoAutenticadoException extends AuthenticationException {

    public UsuarioNoAutenticadoException() {
        super("No se pudo identificar al usuario autenticado");
    }
}
