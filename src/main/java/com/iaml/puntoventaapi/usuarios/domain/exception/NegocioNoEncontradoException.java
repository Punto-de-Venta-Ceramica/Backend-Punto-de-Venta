package com.iaml.puntoventaapi.usuarios.domain.exception;

public class NegocioNoEncontradoException extends RuntimeException {

    public NegocioNoEncontradoException(Long id) {
        super("Negocio no encontrado con id " + id);
    }
}

