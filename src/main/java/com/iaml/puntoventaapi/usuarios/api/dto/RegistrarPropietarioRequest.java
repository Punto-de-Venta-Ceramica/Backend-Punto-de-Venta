package com.iaml.puntoventaapi.usuarios.api.dto;

import com.iaml.puntoventaapi.usuarios.application.RegistrarPropietarioCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarPropietarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 100) String apellidoPaterno,
        @Size(max = 100) String apellidoMaterno,
        @NotBlank @Email @Size(max = 254) String correo,
        @Size(max = 30) String telefono,
        @NotBlank @Size(min = 8, max = 128) String password) {

    public RegistrarPropietarioCommand toCommand() {
        return new RegistrarPropietarioCommand(nombre, apellidoPaterno, apellidoMaterno, correo, telefono, password);
    }
}
