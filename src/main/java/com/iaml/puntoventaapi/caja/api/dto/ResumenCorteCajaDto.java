package com.iaml.puntoventaapi.caja.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ResumenCorteCajaDto(
    Long id,
    Long cajaId,
    Long sucursalId,
    Long usuarioId,
    String usuarioNombre,
    Instant fechaHoraApertura,
    Instant fechaHoraCierre,
    boolean cerrado,
    BigDecimal montoInicial,
    BigDecimal totalVentas,
    BigDecimal totalEstimado,
    BigDecimal totalReal,
    BigDecimal diferencia
) {}

