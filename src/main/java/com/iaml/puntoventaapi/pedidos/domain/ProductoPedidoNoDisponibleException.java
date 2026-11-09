package com.iaml.puntoventaapi.pedidos.domain;

public class ProductoPedidoNoDisponibleException extends IllegalArgumentException {
    public ProductoPedidoNoDisponibleException() {
        super("Uno de los productos ya no está disponible para este pedido.");
    }
}
