package com.iaml.puntoventaapi.catalogo.domain.exception;

public class CatalogoNoEncontradoException extends RuntimeException {

    public CatalogoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
