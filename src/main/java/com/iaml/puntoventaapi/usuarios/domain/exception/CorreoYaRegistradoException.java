package com.iaml.puntoventaapi.usuarios.domain.exception;

public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException() {
        super("El correo ya está registrado por otro usuario");
    }
}
