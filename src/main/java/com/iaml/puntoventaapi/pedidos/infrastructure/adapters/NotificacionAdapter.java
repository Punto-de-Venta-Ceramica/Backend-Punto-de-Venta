package com.iaml.puntoventaapi.pedidos.infrastructure.adapters;

import com.iaml.puntoventaapi.notificaciones.NotificacionesContrato;
import com.iaml.puntoventaapi.pedidos.domain.port.NotificacionPort;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.PedidoJpaRepository;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.PedidoNoEncontradoException;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.stereotype.Component;

@Component
public class NotificacionAdapter implements NotificacionPort {
    private final SucursalContrato sucursales;
    private final NotificacionesContrato notificaciones;
    private final PedidoJpaRepository pedidos;

    public NotificacionAdapter(SucursalContrato sucursales, NotificacionesContrato notificaciones,
                               PedidoJpaRepository pedidos) {
        this.sucursales = sucursales;
        this.notificaciones = notificaciones;
        this.pedidos = pedidos;
    }

    @Override
    public void pedidoRegistrado(long pedidoId, long sucursalId, long usuarioId) {
        var propietarios = sucursales.propietariosDeSucursal(sucursalId);
        if (propietarios.isEmpty()) throw new IllegalStateException("La sucursal no tiene propietario asociado.");
        for (long propietario : propietarios) {
            notificaciones.registrarParaPedido(propietario, pedidoId, "Nuevo pedido", "Se registró un nuevo pedido.");
        }
    }

    @Override
    public void pedidoEntregado(long pedidoId) {
        var pedido = pedidos.findWithUsuarioById(pedidoId).orElseThrow(PedidoNoEncontradoException::new);
        var usuario = pedido.getUsuario();
        var destinatarios = "EMPLEADO".equals(usuario.getRol().getNombre())
            ? java.util.List.of(usuario.getId()) : sucursales.empleadosDeSucursal(pedido.getSucursal().getId());
        for (long empleado : destinatarios) {
            notificaciones.registrarParaPedido(empleado, pedidoId, "Pedido entregado", "Un pedido fue entregado.");
        }
    }
}
