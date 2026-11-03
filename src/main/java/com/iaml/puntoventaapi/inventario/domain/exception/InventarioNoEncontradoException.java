package com.iaml.puntoventaapi.inventario.domain.exception;

public class InventarioNoEncontradoException extends RuntimeException {

    public InventarioNoEncontradoException() {
        super("No se encontró el registro en el inventario.");
    }
}
