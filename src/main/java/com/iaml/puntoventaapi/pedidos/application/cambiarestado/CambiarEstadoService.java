package com.iaml.puntoventaapi.pedidos.application.cambiarestado;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.NotificacionPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CambiarEstadoService {
    private final PedidoRepositorioPort pedidos;
    private final NotificacionPort notificaciones;

    public CambiarEstadoService(PedidoRepositorioPort pedidos, NotificacionPort notificaciones) {
        this.pedidos = pedidos;
        this.notificaciones = notificaciones;
    }

    @Transactional
    public void cambiar(long pedidoId, long usuarioId, EstadoPedido nuevo) {
        if (pedidoId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Pedido o usuario inválido.");
        var pedido = pedidos.obtenerParaActualizar(pedidoId, usuarioId);
        var anterior = pedido.estado();
        boolean propietario = pedidos.esPropietarioActual(pedidoId, usuarioId);
        pedido.cambiarEstado(nuevo, propietario);
        pedidos.guardarEstado(pedidoId, nuevo);
        if (nuevo == EstadoPedido.ENTREGADO && anterior != EstadoPedido.ENTREGADO) {
            notificaciones.pedidoEntregado(pedidoId);
        }
    }
}
