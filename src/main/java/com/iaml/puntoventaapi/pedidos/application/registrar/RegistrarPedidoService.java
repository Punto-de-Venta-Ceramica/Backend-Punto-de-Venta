package com.iaml.puntoventaapi.pedidos.application.registrar;

import com.iaml.puntoventaapi.pedidos.domain.DetallePedido;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import com.iaml.puntoventaapi.pedidos.domain.Pedido;
import com.iaml.puntoventaapi.pedidos.domain.ProductoPedidoNoDisponibleException;
import com.iaml.puntoventaapi.pedidos.domain.TextoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.pedidos.domain.port.ClientePort;
import com.iaml.puntoventaapi.pedidos.domain.port.NotificacionPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/** Orquesta el registro mediante puertos. */
@Service
public class RegistrarPedidoService {
    private final PedidoRepositorioPort pedidos;
    private final CatalogoPort catalogo;
    private final ClientePort clientes;
    private final NotificacionPort notificaciones;

    public RegistrarPedidoService(PedidoRepositorioPort pedidos, CatalogoPort catalogo,
                                  ClientePort clientes, NotificacionPort notificaciones) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.clientes = clientes;
        this.notificaciones = notificaciones;
    }

    public record Resultado(long id, BigDecimal total, BigDecimal totalPagado,
                            BigDecimal saldoPendiente, boolean pagado) { }

    @Transactional
    public Resultado registrar(RegistrarPedidoCommand comando) {
        Objects.requireNonNull(comando, "El pedido es obligatorio.");
        var descripcion = TextoPedido.normalizar(comando.descripcion());
        var descripcionEnvio = TextoPedido.normalizar(comando.descripcionEnvio());
        if (comando.sucursalId() <= 0 || comando.usuarioId() <= 0
                || comando.tipoEntrega() == null || comando.tipoEntrega().isBlank()
                || comando.tipoEntrega().trim().length() > 50
                || (descripcion != null && descripcion.length() > 2000)
                || (descripcionEnvio != null && descripcionEnvio.length() > 2000)
                || TextoPedido.contieneControl(descripcion) || TextoPedido.contieneControl(descripcionEnvio)
                || comando.cliente() == null || comando.productos() == null || comando.productos().isEmpty()
                || comando.productos().size() > 100
                || comando.montoInicial() == null || comando.montoInicial().signum() < 0
                || comando.montoInicial().scale() > 2 || comando.metodoPago() == null) {
            throw new IllegalArgumentException("Los datos del pedido no son válidos.");
        }
        pedidos.comprobarAcceso(comando.sucursalId(), comando.usuarioId());
        var detalles = comando.productos().stream().map(item -> {
            if (item == null || item.productoId() <= 0 || item.cantidad() <= 0) {
                throw new IllegalArgumentException("El producto o la cantidad no son válidos.");
            }
            var producto = catalogo.consultar(comando.sucursalId(), comando.usuarioId(), item.productoId());
            if (producto == null) throw new ProductoPedidoNoDisponibleException();
            return DetallePedido.desdeCatalogo(item.productoId(), item.cantidad(), producto.precio(),
                producto.cantidadMinima(), producto.cantidadMaxima(), producto.descuento());
        }).toList();
        var pedido = new Pedido(detalles);
        if (comando.montoInicial().signum() > 0) {
            pedido.registrarPago(new PagoPedido(comando.montoInicial(), comando.metodoPago()));
        }
        long clienteId = clientes.guardar(comando.cliente());
        var datos = new PedidoRepositorioPort.DatosRegistro(comando.sucursalId(), comando.usuarioId(),
            descripcion, comando.tipoEntrega().trim(), descripcionEnvio);
        long pedidoId = pedidos.guardar(datos, clienteId, pedido);
        notificaciones.pedidoRegistrado(pedidoId, comando.sucursalId(), comando.usuarioId());
        return new Resultado(pedidoId, pedido.total(), pedido.totalPagado(), pedido.saldoPendiente(), pedido.pagado());
    }
}
