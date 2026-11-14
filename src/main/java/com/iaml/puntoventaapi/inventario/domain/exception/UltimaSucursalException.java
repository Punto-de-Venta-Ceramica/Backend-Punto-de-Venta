package com.iaml.puntoventaapi.inventario.domain.exception;

public class UltimaSucursalException extends RuntimeException {

    public UltimaSucursalException() {
        super("El producto debe quedar asignado al menos a una sucursal.");
    }
}
