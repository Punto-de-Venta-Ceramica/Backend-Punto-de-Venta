package com.iaml.puntoventaapi.autenticacion.api.dto;

import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(max = 128) String password) {

    public IniciarSesionCommand toCommand() {
        return new IniciarSesionCommand(correo, password);
    }
}
