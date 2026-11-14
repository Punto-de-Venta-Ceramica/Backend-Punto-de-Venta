package com.iaml.puntoventaapi.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RealizarCorteRequest(
    Long sucursalId,

    @NotNull(message = "El total real es obligatorio.")
    @DecimalMin(value = "0.00", message = "El total real no puede ser negativo.")
    @Digits(integer = 16, fraction = 2, message = "El total real debe tener como máximo 2 decimales.")
    BigDecimal totalReal
) {}
