package com.iaml.puntoventaapi.usuarios.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iaml.puntoventaapi.usuarios.application.EditarEmpleadoCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EditarEmpleadoRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.")
        @JsonProperty("negocioId")
        @JsonAlias({"idNegocio", "NegocioId"})
        Long negocioId,

        @NotNull(message = "El id del empleado es obligatorio.")
        @Positive(message = "El id del empleado debe ser positivo.")
        @JsonProperty("empleadoId")
        @JsonAlias({"idEmpleado", "id", "EmpleadoId"})
        Long empleadoId,

        @Pattern(regexp = "^[\\P{Cc}]*$", message = "El nombre no debe contener caracteres de control.")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres.")
        @JsonProperty("nombre")
        @JsonAlias({"Nombre"})
        String nombre,

        @Pattern(regexp = "^[\\P{Cc}]*$", message = "El apellido paterno no debe contener caracteres de control.")
        @Size(max = 100, message = "El apellido paterno no debe superar 100 caracteres.")
        @JsonProperty("apellidoP")
        @JsonAlias({"apellidoPaterno", "ApellidoP", "ApellidoPaterno"})
        String apellidoPaterno,

        @Pattern(regexp = "^[\\P{Cc}]*$", message = "El apellido materno no debe contener caracteres de control.")
        @Size(max = 100, message = "El apellido materno no debe superar 100 caracteres.")
        @JsonProperty("apellidoM")
        @JsonAlias({"apellidoMaterno", "ApellidoM", "ApellidoMaterno"})
        String apellidoMaterno) {

    public EditarEmpleadoCommand toCommand() {
        return new EditarEmpleadoCommand(negocioId, empleadoId, nombre, apellidoPaterno, apellidoMaterno);
    }
}
