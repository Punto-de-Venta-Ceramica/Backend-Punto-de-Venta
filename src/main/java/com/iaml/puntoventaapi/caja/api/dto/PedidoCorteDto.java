package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Pedido con movimientos en un corte. {@code pagado} y {@code saldo} son al momento de ese corte, así un corte
 * viejo sigue mostrando lo que faltaba entonces. {@code cancelado} indica que se canceló (y devolvió) en el corte.
 */
public record PedidoCorteDto(
    Long id,
    String folio,
    BigDecimal total,
    BigDecimal pagado,
    BigDecimal saldo,
    boolean cancelado,
    List<Pago> pagos,
    List<Producto> productos
) {
    /** {@code tipoPago}: ANTICIPO, LIQUIDACION o DEVOLUCION. */
    public record Pago(String tipoPago, String metodoPago, BigDecimal monto, Instant fechaHora) {}

    public record Producto(String nombre, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}
}
