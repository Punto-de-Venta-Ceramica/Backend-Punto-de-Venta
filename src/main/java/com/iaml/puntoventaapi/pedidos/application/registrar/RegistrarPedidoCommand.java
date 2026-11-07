package com.iaml.puntoventaapi.pedidos.application.registrar;

import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido.MetodoPago;
import java.math.BigDecimal;
import java.util.List;

public record RegistrarPedidoCommand(long sucursalId, long usuarioId,
        String descripcion, ClientePedido cliente, List<ProductoSolicitado> productos,
        String tipoEntrega, String descripcionEnvio, BigDecimal montoInicial, MetodoPago metodoPago) {
    public record ProductoSolicitado(long productoId, int cantidad) { }
}
