package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record VentaConfirmadaResponse(Long ventaId, Long cajaId, Long corteCajaId, Instant fechaHora,
    boolean confirmada, Long usuarioId, Long negocioId, Long sucursalId, MetodoPago metodoPago,
    List<DetalleVentaResponse> detalles, BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
    BigDecimal montoRecibido, BigDecimal cambio) {

    public static VentaConfirmadaResponse from(VentaConfirmada confirmada) {
        var venta = confirmada.venta();
        return new VentaConfirmadaResponse(confirmada.ventaId(), confirmada.cajaId(), confirmada.corteId(),
            confirmada.fechaHora(), true, venta.usuarioId(), venta.negocioId(), venta.sucursalId(), venta.metodoPago(),
            venta.detalles().stream().map(DetalleVentaResponse::from)
                .sorted(Comparator.comparing(DetalleVentaResponse::productoId)).toList(),
            venta.subtotal(), venta.descuento(), venta.total(), venta.montoRecibido(), venta.cambio());
    }
}
