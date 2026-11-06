package com.iaml.puntoventaapi.pedidos.domain;

public class CorteCajaInvalidoException extends RuntimeException {
    public CorteCajaInvalidoException() {
        super("El corte de caja debe estar abierto y pertenecer a la sucursal del pedido.");
    }
}
