package com.iaml.puntoventaapi.pedidos.application.cancelar;

import com.iaml.puntoventaapi.pedidos.application.cambiarestado.CambiarEstadoService;
import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CancelarPedidoService {
    private final CambiarEstadoService estados;
    public CancelarPedidoService(CambiarEstadoService estados) { this.estados = estados; }

    @Transactional
    public void cancelar(long pedidoId, long usuarioId) {
        estados.cambiar(pedidoId, usuarioId, EstadoPedido.CANCELADO);
    }
}
