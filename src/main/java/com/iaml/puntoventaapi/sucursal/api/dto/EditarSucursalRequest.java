package com.iaml.puntoventaapi.sucursal.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iaml.puntoventaapi.sucursal.application.EditarSucursalCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EditarSucursalRequest(
        @NotNull(message = "El id de la sucursal es obligatorio.")
        @Positive(message = "El id de la sucursal debe ser positivo.") Long id,
        @Pattern(regexp = "(?s).*\\S.*", message = "El nombre no puede estar vacío ni contener solo espacios.")
        @Pattern(regexp = "^[\\P{Cc}]*$", message = "El nombre no debe contener saltos de línea ni caracteres de control.")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres.") String nombre,
        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres.")
        @Pattern(regexp = "^[\\P{Cc}\\r\\n\\t]*$", message = "La descripción contiene caracteres de control no permitidos.") String descripcion) {

    @JsonIgnore
    @AssertTrue(message = "Envía al menos un campo editable: nombre o descripcion.")
    public boolean isConCambios() {
        return nombre != null || descripcion != null;
    }

    @JsonAnySetter
    public void rechazarCampoNoEditable(String campo, Object valor) {
        throw new IllegalArgumentException("El campo '" + campo + "' no es editable.");
    }

    public EditarSucursalCommand toCommand() {
        return new EditarSucursalCommand(id, nombre, descripcion);
    }
}

