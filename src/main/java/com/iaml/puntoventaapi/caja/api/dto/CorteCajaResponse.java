package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CorteCajaResponse(
    Long id,
    Long cajaId,
    Long sucursalId,
    String sucursalNombre,
    Long usuarioId,
    String usuarioNombre,
    Instant fechaHoraApertura,
    Instant fechaHoraCierre,
    boolean cerrado,
    BigDecimal montoInicial,
    BigDecimal totalVentas,
    BigDecimal totalAnticipos,
    BigDecimal totalLiquidaciones,
    BigDecimal totalCancelaciones,
    DesgloseMetodoPagoDto ventasEfectivo,
    DesgloseMetodoPagoDto ventasTarjeta,
    SeccionCancelacionesDto cancelacionesPedidos,
    BigDecimal totalEfectivo,
    BigDecimal totalTarjeta,
    BigDecimal totalEstimado,
    BigDecimal totalReal,
    BigDecimal diferencia,
    List<ProductoVendidoDto> productos
) {}
