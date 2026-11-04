package com.iaml.puntoventaapi.pedidos.domain.port;

public interface NotificacionPort {
    void pedidoRegistrado(long pedidoId, long sucursalId, long usuarioId);
    void pedidoEntregado(long pedidoId);
}
