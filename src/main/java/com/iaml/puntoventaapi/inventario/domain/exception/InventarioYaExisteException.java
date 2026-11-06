package com.iaml.puntoventaapi.inventario.domain.exception;

public class InventarioYaExisteException extends RuntimeException {

    public InventarioYaExisteException() {
        super("El producto ya está asignado a esa sucursal.");
    }
}
