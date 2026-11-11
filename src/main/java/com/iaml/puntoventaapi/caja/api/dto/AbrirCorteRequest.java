package com.iaml.puntoventaapi.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AbrirCorteRequest(
    Long sucursalId,

    @NotNull(message = "El monto inicial es obligatorio.")
    @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo.")
    @Digits(integer = 16, fraction = 2, message = "El monto inicial debe tener como máximo 2 decimales.")
    BigDecimal montoInicial
) {}

