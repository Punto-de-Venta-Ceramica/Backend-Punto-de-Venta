package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.Venta;
import java.math.BigDecimal;
import java.util.List;

/** No expone id de venta: la preparación no escribe en la base de datos. */
public record VentaResponse(boolean confirmada, Long usuarioId, Long negocioId, Long sucursalId,
                            MetodoPago metodoPago, List<DetalleVentaResponse> detalles,
                            BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
                            BigDecimal montoRecibido, BigDecimal cambio) {

    public static VentaResponse from(Venta venta) {
        return new VentaResponse(false, venta.usuarioId(), venta.negocioId(), venta.sucursalId(), venta.metodoPago(),
            venta.detalles().stream().map(DetalleVentaResponse::from).toList(),
            venta.subtotal(), venta.descuento(), venta.total(), venta.montoRecibido(), venta.cambio());
    }
}
