package com.iaml.puntoventaapi.pedidos.application.consultar;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPedidosService {
    /** Pedidos por página del listado. */
    public static final int TAMANIO_PAGINA = 20;

    private final ConsultaPedidosPort pedidos;
    public ConsultarPedidosService(ConsultaPedidosPort pedidos) { this.pedidos = pedidos; }

    @Transactional(readOnly = true)
    public ConsultaPedidosPort.Vista consultar(long pedidoId, long usuarioId) {
        if (pedidoId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Pedido o usuario inválido.");
        return pedidos.consultar(pedidoId, usuarioId);
    }

    @Transactional(readOnly = true)
    public Pagina<ConsultaPedidosPort.Resumen> listar(long sucursalId, long usuarioId, EstadoPedido estado,
                                                      int pagina) {
        if (sucursalId <= 0 || usuarioId <= 0 || pagina < 0) {
            throw new IllegalArgumentException("Sucursal, usuario o página inválidos.");
        }
        return pedidos.listar(sucursalId, usuarioId, estado, pagina, TAMANIO_PAGINA);
    }
}
