package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CancelacionPedidoDto(
    Long pedidoId,
    String folio,
    BigDecimal monto,
    String metodoPago,
    String tipoPago,
    Instant fechaHora
) {}

