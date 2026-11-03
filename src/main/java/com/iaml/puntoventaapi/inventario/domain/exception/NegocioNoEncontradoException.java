package com.iaml.puntoventaapi.inventario.domain.exception;

public class NegocioNoEncontradoException extends RuntimeException {

    public NegocioNoEncontradoException() {
        super("No se encontró el negocio.");
    }
}
