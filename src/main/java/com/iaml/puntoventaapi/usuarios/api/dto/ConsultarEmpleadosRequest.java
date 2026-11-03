package com.iaml.puntoventaapi.usuarios.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsultarEmpleadosRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.")
        @JsonProperty("negocioId")
        @JsonAlias({"idNegocio", "NegocioId"})
        Long negocioId,

        @JsonProperty("incluirAsignacion")
        @JsonAlias({"asignacion", "conAsignacion"})
        Boolean incluirAsignacion) {
}

