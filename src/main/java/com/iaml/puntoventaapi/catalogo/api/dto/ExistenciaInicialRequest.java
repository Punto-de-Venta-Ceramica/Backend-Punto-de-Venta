package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.application.ExistenciaInicialCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExistenciaInicialRequest(
        @NotNull(message = "El id de la sucursal es obligatorio.")
        @Positive(message = "El id de la sucursal debe ser positivo.") Long sucursalId,
        @NotNull(message = "La existencia inicial es obligatoria.")
        @Min(value = 0, message = "La existencia inicial no puede ser negativa.") Integer existencia) {

    public ExistenciaInicialCommand toCommand() {
        return new ExistenciaInicialCommand(sucursalId, existencia);
    }
}
