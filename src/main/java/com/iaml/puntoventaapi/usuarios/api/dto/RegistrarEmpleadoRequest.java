package com.iaml.puntoventaapi.usuarios.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iaml.puntoventaapi.usuarios.application.RegistrarEmpleadoCommand;
import jakarta.validation.constraints.*;

public record RegistrarEmpleadoRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.")
        @JsonProperty("negocioId")
        @JsonAlias({"idNegocio", "NegocioId"})
        Long negocioId,

        @NotBlank(message = "El nombre es obligatorio y no puede contener solo espacios.")
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
        String apellidoMaterno,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Pattern(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$", message = "El formato del correo electrónico no es válido.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 254, message = "El correo electrónico no debe superar 254 caracteres.")
        @JsonProperty("correoElectronico")
        @JsonAlias({"correo", "CorreoElectronico", "CorreoEectronico", "correoEectronico"})
        String correoElectronico,

        @Positive(message = "El id de la sucursal debe ser positivo si se proporciona.")
        @JsonProperty("sucursalId")
        @JsonAlias({"idSucursal", "SucursalId"})
        Long sucursalId) {

    public RegistrarEmpleadoCommand toCommand() {
        return new RegistrarEmpleadoCommand(negocioId, nombre, apellidoPaterno, apellidoMaterno, correoElectronico, sucursalId);
    }
}
