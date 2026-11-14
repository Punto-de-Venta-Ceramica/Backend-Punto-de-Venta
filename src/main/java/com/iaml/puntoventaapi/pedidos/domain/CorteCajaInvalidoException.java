package com.iaml.puntoventaapi.pedidos.domain;

public class CorteCajaInvalidoException extends RuntimeException {
    public CorteCajaInvalidoException() {
        this("No hay un corte de caja abierto en esta sucursal. Abre uno antes de registrar un pago.");
    }

    public CorteCajaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
