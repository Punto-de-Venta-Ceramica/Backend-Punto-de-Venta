package com.iaml.puntoventaapi.usuarios.api.dto;

import com.iaml.puntoventaapi.usuarios.application.ActualizarSucursalesEmpleadoCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ActualizarSucursalesEmpleadoRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.")
        Long negocioId,

        @NotNull(message = "El id del empleado es obligatorio.")
        @Positive(message = "El id del empleado debe ser positivo.")
        Long empleadoId,

        @NotNull(message = "La lista de sucursales es obligatoria (puede estar vacía).")
        List<@NotNull(message = "El id de la sucursal es obligatorio.")
                @Positive(message = "El id de la sucursal debe ser positivo.") Long> sucursalIds) {

    public ActualizarSucursalesEmpleadoCommand toCommand() {
        return new ActualizarSucursalesEmpleadoCommand(negocioId, empleadoId, sucursalIds);
    }
}
