package com.iaml.puntoventaapi.catalogo.domain.exception;

/** Violación de una regla de negocio al registrar o editar un producto (sucursales, categoría, etc.). */
public class ProductoInvalidoException extends RuntimeException {

    public ProductoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
