package com.iaml.puntoventaapi.catalogo.domain.exception;

public class CategoriaNoEncontradaException extends RuntimeException {

    public CategoriaNoEncontradaException() {
        super("No se encontró la categoría.");
    }
}
