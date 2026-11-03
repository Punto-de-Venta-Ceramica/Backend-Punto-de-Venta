package com.iaml.puntoventaapi.usuarios.domain.exception;

public class EmpleadoConDependenciasException extends RuntimeException {

    public EmpleadoConDependenciasException(Throwable cause) {
        super("No se puede eliminar el empleado porque tiene registros de operaciones vinculadas (ventas, pedidos o cortes).", cause);
    }
}

