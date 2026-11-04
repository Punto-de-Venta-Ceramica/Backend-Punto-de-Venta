package com.iaml.puntoventaapi.pedidos.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record PagoPedido(BigDecimal monto, MetodoPago metodoPago) {
    public enum MetodoPago { EFECTIVO, TARJETA }

    public PagoPedido {
        Objects.requireNonNull(monto, "El monto es obligatorio.");
        Objects.requireNonNull(metodoPago, "El método de pago es obligatorio.");
        if (monto.signum() <= 0 || monto.scale() > 2) {
            throw new IllegalArgumentException("El pago debe ser mayor a cero y tener hasta dos decimales.");
        }
    }
}
