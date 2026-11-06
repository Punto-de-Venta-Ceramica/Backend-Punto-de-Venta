package com.iaml.puntoventaapi.sucursal.domain.exception;

public class SucursalNoEncontradaException extends RuntimeException {

    public SucursalNoEncontradaException(Long id) {
        super("No existe la sucursal con id " + id + ".");
    }
}

