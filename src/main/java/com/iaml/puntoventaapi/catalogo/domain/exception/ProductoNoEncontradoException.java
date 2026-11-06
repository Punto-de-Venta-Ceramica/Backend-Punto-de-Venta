package com.iaml.puntoventaapi.catalogo.domain.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException() {
        super("No se encontró el producto.");
    }
}
