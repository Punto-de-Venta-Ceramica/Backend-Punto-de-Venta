package com.iaml.puntoventaapi.pedidos.application.editar;

import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ClientePort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarDatosPedidoService {
    private final PedidoRepositorioPort pedidos;
    private final ClientePort clientes;
    public EditarDatosPedidoService(PedidoRepositorioPort pedidos, ClientePort clientes) {
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    /** Actualiza los datos de la orden sin alterar artículos ni pagos históricos. */
    @Transactional
    public void editar(long pedidoId, long usuarioId, ClientePedido cliente,
                       PedidoRepositorioPort.DatosEdicion datos) {
        if (pedidoId <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Pedido o usuario inválido.");
        Objects.requireNonNull(cliente, "El cliente es obligatorio.");
        Objects.requireNonNull(datos, "Los datos son obligatorios.");
        if (datos.tipoEntrega() == null || datos.tipoEntrega().isBlank() || datos.tipoEntrega().length() > 50
            || (datos.descripcion() != null && datos.descripcion().length() > 2000)
            || (datos.descripcionEnvio() != null && datos.descripcionEnvio().length() > 2000)) {
            throw new IllegalArgumentException("Los datos del pedido no son válidos.");
        }
        var pedido = pedidos.obtenerParaActualizar(pedidoId, usuarioId);
        if (!pedidos.esPropietarioActual(pedidoId, usuarioId)) throw new AccessDeniedException("Acceso denegado.");
        if (pedido.estado() == EstadoPedido.CANCELADO) throw new IllegalStateException("El pedido está cancelado.");
        // Los clientes pueden ser compartidos por otros pedidos; se conserva su información histórica.
        long clienteId = clientes.guardar(cliente);
        pedidos.actualizarDatos(pedidoId, clienteId, new PedidoRepositorioPort.DatosEdicion(
            datos.descripcion(), datos.tipoEntrega().trim(), datos.descripcionEnvio()));
    }
}
