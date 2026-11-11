package com.iaml.puntoventaapi.caja.domain.exception;

public class CorteCajaNoAbiertoException extends RuntimeException {
    public CorteCajaNoAbiertoException(String mensaje) {
        super(mensaje);
    }
}

