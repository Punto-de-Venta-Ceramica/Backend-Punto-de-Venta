package com.iaml.puntoventaapi.inventario.domain.exception;

public class ProductoOSucursalNoEncontradoException extends RuntimeException {

    public ProductoOSucursalNoEncontradoException() {
        super("No se encontró el producto o la sucursal en tu negocio.");
    }
}
