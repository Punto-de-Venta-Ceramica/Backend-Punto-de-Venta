package com.iaml.puntoventaapi.catalogo.domain.exception;

/** Violación de una regla de negocio al registrar o editar un producto (sucursales, categoría, imagen, etc.). */
public class ProductoInvalidoException extends IllegalArgumentException {

    public ProductoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
