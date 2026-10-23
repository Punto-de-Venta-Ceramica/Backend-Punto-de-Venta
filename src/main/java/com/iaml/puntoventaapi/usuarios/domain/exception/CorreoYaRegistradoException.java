package com.iaml.puntoventaapi.usuarios.domain.exception;

public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException() {
        super("El correo electrónico ya está registrado.");
    }

    public CorreoYaRegistradoException(Throwable cause) {
        super("El correo electrónico ya está registrado.", cause);
    }
}
