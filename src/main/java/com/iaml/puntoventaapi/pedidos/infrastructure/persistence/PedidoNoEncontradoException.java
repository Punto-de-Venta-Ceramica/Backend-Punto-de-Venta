package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException() { super("El pedido no existe o no está disponible."); }
}
