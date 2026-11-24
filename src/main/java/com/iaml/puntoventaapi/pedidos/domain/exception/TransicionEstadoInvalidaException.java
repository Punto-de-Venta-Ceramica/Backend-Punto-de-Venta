package com.iaml.puntoventaapi.pedidos.domain.exception;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;

/**
 * Cambio de estado que no respeta el orden Encargado → En producción → Entregado → Terminado. El mensaje
 * dice en qué estado está el pedido y a cuál puede pasar, para mostrarlo tal cual al usuario (409).
 */
public class TransicionEstadoInvalidaException extends IllegalStateException {
    private final EstadoPedido estadoActual;

    public TransicionEstadoInvalidaException(EstadoPedido estadoActual) {
        super(mensaje(estadoActual));
        this.estadoActual = estadoActual;
    }

    public EstadoPedido getEstadoActual() {
        return estadoActual;
    }

    private static String mensaje(EstadoPedido actual) {
        return switch (actual) {
            case CANCELADO -> "El pedido está cancelado y ya no puede cambiar de estado.";
            case TERMINADO -> "El pedido ya está terminado y no puede cambiar de estado.";
            case ENCARGADO -> "El pedido está en Encargado; solo puede pasar a En producción o cancelarse.";
            case PRODUCCION -> "El pedido está en En producción; solo puede pasar a Entregado o cancelarse.";
            case ENTREGADO -> "El pedido está en Entregado; solo puede pasar a Terminado o cancelarse.";
        };
    }
}
