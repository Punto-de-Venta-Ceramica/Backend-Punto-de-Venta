package com.iaml.puntoventaapi.usuarios.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
    @NotBlank(message = "El nombre es obligatorio y no puede contener solo espacios.")
    @Pattern(regexp = "^[^0-9]+$", message = "El nombre no debe contener números.")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres.")
    String nombre,

    @NotBlank(message = "El apellido paterno es obligatorio y no puede contener solo espacios.")
    @Pattern(regexp = "^[^0-9]+$", message = "El apellido paterno no debe contener números.")
    @Size(max = 100, message = "El apellido paterno no debe superar 100 caracteres.")
    String apellidoPaterno,

    @Pattern(regexp = "^[^0-9]*$", message = "El apellido materno no debe contener números.")
    @Size(max = 100, message = "El apellido materno no debe superar 100 caracteres.")
    String apellidoMaterno,

    @NotBlank(message = "El teléfono es obligatorio y no puede contener solo espacios.")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos numéricos.")
    String telefono,

    @Email(message = "El correo electrónico no tiene un formato válido.")
    @Size(max = 254, message = "El correo electrónico no debe superar 254 caracteres.")
    String correo
) {}

