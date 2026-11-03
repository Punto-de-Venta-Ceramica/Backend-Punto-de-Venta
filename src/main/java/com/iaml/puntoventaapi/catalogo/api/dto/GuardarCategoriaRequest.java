package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.application.GuardarCategoriaCommand;
import com.iaml.puntoventaapi.catalogo.domain.DatosClasificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarCategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = DatosClasificacion.MAX_NOMBRE, message = "El nombre no debe superar 100 caracteres.")
        String nombre,
        String descripcion) {

    public GuardarCategoriaCommand toCommand() {
        return new GuardarCategoriaCommand(nombre, descripcion);
    }
}
