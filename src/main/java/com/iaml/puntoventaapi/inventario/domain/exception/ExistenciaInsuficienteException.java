package com.iaml.puntoventaapi.inventario.domain.exception;

public class ExistenciaInsuficienteException extends RuntimeException {

    public ExistenciaInsuficienteException() {
        super("La existencia no puede quedar por debajo de cero.");
    }
}
