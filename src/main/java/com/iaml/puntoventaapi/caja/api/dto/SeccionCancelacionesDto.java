package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record SeccionCancelacionesDto(
    BigDecimal totalCancelaciones,
    BigDecimal cancelacionesEfectivo,
    BigDecimal cancelacionesTarjeta,
    List<CancelacionPedidoDto> detalles
) {}

