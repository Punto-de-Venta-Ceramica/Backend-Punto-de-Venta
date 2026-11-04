package com.iaml.puntoventaapi.pedidos.application.consultar;

import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPedidosService {
    private final ConsultaPedidosPort pedidos;
    public ConsultarPedidosService(ConsultaPedidosPort pedidos) { this.pedidos = pedidos; }

    @Transactional(readOnly = true)
    public ConsultaPedidosPort.Vista consultar(long pedidoId, long usuarioId) {
        if (pedidoId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Pedido o usuario inválido.");
        return pedidos.consultar(pedidoId, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<ConsultaPedidosPort.Vista> listar(long sucursalId, long usuarioId) {
        if (sucursalId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Sucursal o usuario inválido.");
        return pedidos.listar(sucursalId, usuarioId);
    }
}
