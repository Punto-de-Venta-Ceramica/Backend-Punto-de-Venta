package com.iaml.puntoventaapi.pedidos.domain;

public class CorteCajaInvalidoException extends RuntimeException {
    public CorteCajaInvalidoException() {
        super("Debe haber un corte de caja abierto para la sucursal del pedido.");
    }
}
