package com.iaml.puntoventaapi.sucursal.domain.exception;

public class NegocioNoEncontradoException extends RuntimeException {

    public NegocioNoEncontradoException(Long id) {
        super("No existe el negocio con id " + id + ".");
    }
}

