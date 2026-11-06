package com.iaml.puntoventaapi.usuarios.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iaml.puntoventaapi.usuarios.application.EliminarEmpleadoCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EliminarEmpleadoRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.")
        @JsonProperty("negocioId")
        @JsonAlias({"idNegocio", "NegocioId"})
        Long negocioId,

        @NotNull(message = "El id del empleado es obligatorio.")
        @Positive(message = "El id del empleado debe ser positivo.")
        @JsonProperty("empleadoId")
        @JsonAlias({"idEmpleado", "id", "EmpleadoId"})
        Long empleadoId) {

    public EliminarEmpleadoCommand toCommand() {
        return new EliminarEmpleadoCommand(negocioId, empleadoId);
    }
}

