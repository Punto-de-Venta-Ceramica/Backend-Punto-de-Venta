package com.iaml.puntoventaapi.ventas.domain;

import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resultado de preparar una venta; todavía no es una operación confirmada ni persistida. */
public record Venta(Long usuarioId, Long negocioId, Long sucursalId, MetodoPago metodoPago,
                    List<DetalleVenta> detalles, BigDecimal montoRecibido) {

    private static final BigDecimal CERO = new BigDecimal("0.00");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("9999999999999999.99");

    public Venta {
        if (usuarioId == null || usuarioId <= 0 || negocioId == null || negocioId <= 0
                || sucursalId == null || sucursalId <= 0 || metodoPago == null
                || detalles == null || detalles.isEmpty()) {
            throw new DatosVentaInvalidosException(Map.of("venta", "Los datos de la venta no son válidos."));
        }
        detalles = List.copyOf(detalles);
        // Reutiliza las reglas del carrito para evitar estados inválidos al construir el dominio directamente.
        new SolicitudVenta(sucursalId, metodoPago,
            detalles.stream().map(d -> new ProductoSolicitado(d.productoId(), d.cantidad())).toList(), montoRecibido);
        BigDecimal total = detalles.stream().map(DetalleVenta::subtotal).reduce(CERO, BigDecimal::add);
        if (total.compareTo(TOTAL_MAXIMO) > 0) {
            throw new DatosVentaInvalidosException(Map.of("productos", "El total de la venta supera el máximo permitido."));
        }
        if (montoRecibido != null) {
            if (montoRecibido.compareTo(total) < 0) {
                throw new DatosVentaInvalidosException(Map.of("montoRecibido", "El monto recibido no cubre el total de la venta."));
            }
            montoRecibido = montoRecibido.setScale(2);
        }
    }

    public BigDecimal subtotal() {
        return detalles.stream().map(DetalleVenta::importeBruto).reduce(CERO, BigDecimal::add);
    }

    public BigDecimal descuento() {
        return detalles.stream().map(DetalleVenta::descuento).reduce(CERO, BigDecimal::add);
    }

    public BigDecimal total() {
        return detalles.stream().map(DetalleVenta::subtotal).reduce(CERO, BigDecimal::add);
    }

    public BigDecimal cambio() {
        return montoRecibido == null ? null : montoRecibido.subtract(total());
    }
}
