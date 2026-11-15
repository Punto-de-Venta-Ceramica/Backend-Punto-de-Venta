package com.iaml.puntoventaapi.pedidos.domain;

public class PagoExcedeSaldoException extends IllegalArgumentException {
    public PagoExcedeSaldoException() {
        super("El pago no puede superar el saldo pendiente del pedido.");
    }
}
