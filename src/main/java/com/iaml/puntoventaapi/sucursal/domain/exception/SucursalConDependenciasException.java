package com.iaml.puntoventaapi.sucursal.domain.exception;

public class SucursalConDependenciasException extends RuntimeException {

    public SucursalConDependenciasException(Throwable cause) {
        super("No se puede eliminar la sucursal porque tiene registros relacionados.", cause);
    }
}

