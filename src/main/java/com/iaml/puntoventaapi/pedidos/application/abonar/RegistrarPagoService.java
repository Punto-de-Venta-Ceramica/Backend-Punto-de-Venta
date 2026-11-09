package com.iaml.puntoventaapi.pedidos.application.abonar;

import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RegistrarPagoService {
    private final PedidoRepositorioPort pedidos;
    public RegistrarPagoService(PedidoRepositorioPort pedidos) { this.pedidos = pedidos; }

    public record Resultado(BigDecimal totalPagado, BigDecimal saldoPendiente, boolean pagado) { }

    @Transactional
    public Resultado registrar(long pedidoId, long usuarioId, PagoPedido pago) {
        if (pedidoId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Pedido o usuario inválido.");
        Objects.requireNonNull(pago, "El pago es obligatorio.");
        var pedido = pedidos.obtenerParaActualizar(pedidoId, usuarioId);
        pedido.registrarPago(pago);
        pedidos.guardarPago(pedidoId, pago);
        return new Resultado(pedido.totalPagado(), pedido.saldoPendiente(), pedido.pagado());
    }
}
