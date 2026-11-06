package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.application.GuardarAreaCommand;
import com.iaml.puntoventaapi.catalogo.domain.DatosClasificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarAreaRequest(
        @NotBlank(message = "El nombre del área es obligatorio.")
        @Size(max = DatosClasificacion.MAX_NOMBRE, message = "El nombre no debe superar 100 caracteres.")
        String nombre,
        String descripcion) {

    public GuardarAreaCommand toCommand() {
        return new GuardarAreaCommand(nombre, descripcion);
    }
}
