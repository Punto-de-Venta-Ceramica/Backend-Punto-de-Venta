package com.iaml.puntoventaapi.pedidos.domain;

/** El estado de pago se calcula por separado del progreso de producción. */
public enum EstadoPedido {
    ENCARGADO, PRODUCCION, ENTREGADO, TERMINADO, CANCELADO
}
