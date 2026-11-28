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
    /** Nombre del cliente: hasta 150 caracteres. */
    static final int MAXIMO_BUSQUEDA = 150;

    private final ConsultaPedidosPort pedidos;
    public ConsultarPedidosService(ConsultaPedidosPort pedidos) { this.pedidos = pedidos; }

    @Transactional(readOnly = true)
    public ConsultaPedidosPort.Vista consultar(long pedidoId, long usuarioId) {
        if (pedidoId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Pedido o usuario inválido.");
        return pedidos.consultar(pedidoId, usuarioId);
    }

    @Transactional(readOnly = true)
    public Pagina<ConsultaPedidosPort.Resumen> listar(long sucursalId, long usuarioId, EstadoPedido estado,
                                                      String busqueda, int pagina) {
        if (sucursalId <= 0 || usuarioId <= 0 || pagina < 0) {
            throw new IllegalArgumentException("Sucursal, usuario o página inválidos.");
        }
        String texto = busqueda == null || busqueda.isBlank() ? null : busqueda.strip();
        if (texto != null && texto.length() > MAXIMO_BUSQUEDA) {
            throw new IllegalArgumentException("La búsqueda es demasiado larga.");
        }
        return pedidos.listar(sucursalId, usuarioId, estado, texto, pagina, TAMANIO_PAGINA);
    }
}
