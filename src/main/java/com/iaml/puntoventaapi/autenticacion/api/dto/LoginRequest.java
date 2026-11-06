package com.iaml.puntoventaapi.autenticacion.api.dto;

import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 254, message = "El correo electrónico no debe superar 254 caracteres.") String correo,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 128, message = "La contraseña no debe superar 128 caracteres.") String password) {

    public IniciarSesionCommand toCommand() {
        return new IniciarSesionCommand(correo, password);
    }
}
