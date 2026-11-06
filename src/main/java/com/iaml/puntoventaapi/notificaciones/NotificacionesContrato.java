package com.iaml.puntoventaapi.notificaciones;

public interface NotificacionesContrato {
    /** Guarda el aviso en la misma transacción que el pedido. */
    void registrarParaPedido(long destinatarioId, long pedidoId, String titulo, String mensaje);
}
