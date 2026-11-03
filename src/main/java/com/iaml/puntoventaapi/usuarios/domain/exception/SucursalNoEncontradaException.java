package com.iaml.puntoventaapi.usuarios.domain.exception;

public class SucursalNoEncontradaException extends RuntimeException {

    public SucursalNoEncontradaException(Long id) {
        super("Sucursal no encontrada con id " + id);
    }
}

