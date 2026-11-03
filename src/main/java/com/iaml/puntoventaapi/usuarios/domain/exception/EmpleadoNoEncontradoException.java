package com.iaml.puntoventaapi.usuarios.domain.exception;

public class EmpleadoNoEncontradoException extends RuntimeException {

    public EmpleadoNoEncontradoException(Long id) {
        super("Empleado no encontrado con id " + id);
    }
}

