package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;

public record DesgloseMetodoPagoDto(
    BigDecimal totalVentas,
    BigDecimal anticipos,
    BigDecimal liquidaciones,
    BigDecimal cancelaciones,
    BigDecimal subtotalNeto,
    BigDecimal montoInicial,
    BigDecimal totalEsperado
) {}

