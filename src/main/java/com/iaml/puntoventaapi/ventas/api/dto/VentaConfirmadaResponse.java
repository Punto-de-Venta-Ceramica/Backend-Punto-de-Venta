package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record VentaConfirmadaResponse(Long ventaId, String folio, Long cajaId, Long corteCajaId, Instant fechaHora,
    boolean confirmada, Long usuarioId, Long negocioId, Long sucursalId, MetodoPago metodoPago,
    List<DetalleVentaResponse> detalles, BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
    BigDecimal montoRecibido, BigDecimal cambio, String negocioEslogan) {

    /** negocioEslogan: frase del negocio para el pie del ticket; null si no tiene. */
    public static VentaConfirmadaResponse from(VentaConfirmada confirmada, String negocioEslogan) {
        var venta = confirmada.venta();
        return new VentaConfirmadaResponse(confirmada.ventaId(), confirmada.folio(), confirmada.cajaId(), confirmada.corteId(),
            confirmada.fechaHora(), true, venta.usuarioId(), venta.negocioId(), venta.sucursalId(), venta.metodoPago(),
            venta.detalles().stream().map(DetalleVentaResponse::from)
                .sorted(Comparator.comparing(DetalleVentaResponse::productoId)).toList(),
            venta.subtotal(), venta.descuento(), venta.total(), venta.montoRecibido(), venta.cambio(), negocioEslogan);
    }
}
