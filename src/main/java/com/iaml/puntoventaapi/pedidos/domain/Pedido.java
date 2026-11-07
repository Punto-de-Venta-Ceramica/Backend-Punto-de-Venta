package com.iaml.puntoventaapi.pedidos.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Reglas de negocio independientes de HTTP y de la persistencia. */
public final class Pedido {
    private final List<DetallePedido> detalles;
    private final List<PagoPedido> pagos;
    private EstadoPedido estado;

    public Pedido(List<DetallePedido> detalles) {
        Objects.requireNonNull(detalles, "Los productos son obligatorios.");
        if (detalles.isEmpty() || detalles.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("El pedido debe contener al menos un producto válido.");
        }
        var ids = new HashSet<Long>();
        if (detalles.stream().anyMatch(detalle -> !ids.add(detalle.productoId()))) {
            throw new IllegalArgumentException("Un producto no puede repetirse en el pedido.");
        }
        this.detalles = List.copyOf(detalles);
        this.pagos = new ArrayList<>();
        this.estado = EstadoPedido.ENCARGADO;
    }

    /** Reconstruye un pedido persistido conservando la validación de sus pagos. */
    public static Pedido restaurar(List<DetallePedido> detalles, List<PagoPedido> pagos, EstadoPedido estado) {
        Objects.requireNonNull(pagos, "Los pagos son obligatorios.");
        Objects.requireNonNull(estado, "El estado es obligatorio.");
        var pedido = new Pedido(detalles);
        pagos.forEach(pedido::registrarPago);
        pedido.estado = estado;
        return pedido;
    }

    public BigDecimal total() {
        return detalles.stream().map(DetallePedido::subtotal)
            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public BigDecimal totalPagado() {
        return pagos.stream().map(PagoPedido::monto)
            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public BigDecimal saldoPendiente() { return total().subtract(totalPagado()); }
    public boolean pagado() { return saldoPendiente().signum() == 0; }
    public EstadoPedido estado() { return estado; }
    public List<DetallePedido> detalles() { return detalles; }
    public List<PagoPedido> pagos() { return List.copyOf(pagos); }

    public void registrarPago(PagoPedido pago) {
        Objects.requireNonNull(pago, "El pago es obligatorio.");
        if (estado == EstadoPedido.CANCELADO) {
            throw new IllegalArgumentException("No es posible registrar ese pago para el pedido.");
        }
        if (pago.monto().compareTo(saldoPendiente()) > 0) {
            throw new PagoExcedeSaldoException();
        }
        pagos.add(pago);
    }

    public void cambiarEstado(EstadoPedido nuevo, boolean propietario) {
        Objects.requireNonNull(nuevo, "El estado es obligatorio.");
        if (estado == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("Un pedido cancelado no puede cambiar de estado.");
        }
        if ((nuevo == EstadoPedido.PRODUCCION || nuevo == EstadoPedido.CANCELADO) && !propietario) {
            throw new SecurityException("Solo el propietario puede realizar esta acción.");
        }
        if (nuevo == estado) return;
        boolean permitido = nuevo == EstadoPedido.CANCELADO || switch (estado) {
            case ENCARGADO -> nuevo == EstadoPedido.PRODUCCION;
            case PRODUCCION -> nuevo == EstadoPedido.ENTREGADO;
            case ENTREGADO -> nuevo == EstadoPedido.TERMINADO;
            case TERMINADO, CANCELADO -> false;
        };
        if (!permitido) throw new IllegalStateException("El cambio de estado no está permitido.");
        estado = nuevo;
    }
}
