package com.iaml.puntoventaapi.ventas.domain.exception;

public class ConflictoConfirmacionVentaException extends RuntimeException {
    public ConflictoConfirmacionVentaException(String mensaje) {
        super(mensaje);
    }
}
