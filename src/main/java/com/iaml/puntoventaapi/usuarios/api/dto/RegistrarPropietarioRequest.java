package com.iaml.puntoventaapi.usuarios.api.dto;

import com.iaml.puntoventaapi.usuarios.application.RegistrarPropietarioCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RegistrarPropietarioRequest(
        @NotBlank(message = "El nombre es obligatorio y no puede contener solo espacios.")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres.") String nombre,
        @Size(max = 100, message = "El apellido paterno no debe superar 100 caracteres.") String apellidoPaterno,
        @Size(max = 100, message = "El apellido materno no debe superar 100 caracteres.") String apellidoMaterno,
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 254, message = "El correo electrónico no debe superar 254 caracteres.") String correo,
        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos.") String telefono,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 128, message = "La contraseña debe contener entre 8 y 128 caracteres.") String password) {

    public RegistrarPropietarioCommand toCommand() {
        return new RegistrarPropietarioCommand(nombre, apellidoPaterno, apellidoMaterno, correo, telefono, password);
    }
}
