package com.iaml.puntoventaapi.caja.domain.exception;

public class CorteCajaNoEncontradoException extends RuntimeException {
    public CorteCajaNoEncontradoException(Long id) {
        super("No se encontró el corte de caja con ID: " + id);
    }
}

