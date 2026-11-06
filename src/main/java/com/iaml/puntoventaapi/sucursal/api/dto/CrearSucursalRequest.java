package com.iaml.puntoventaapi.sucursal.api.dto;

import com.iaml.puntoventaapi.sucursal.application.CrearSucursalCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearSucursalRequest(
        @NotNull(message = "El id del negocio es obligatorio.")
        @Positive(message = "El id del negocio debe ser positivo.") Long negocioId,
        @NotBlank(message = "El nombre es obligatorio y no puede contener solo espacios.")
        @Pattern(regexp = "^[\\P{Cc}]*$", message = "El nombre no debe contener saltos de línea ni caracteres de control.")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres.") String nombre,
        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres.")
        @Pattern(regexp = "^[\\P{Cc}\\r\\n\\t]*$", message = "La descripción contiene caracteres de control no permitidos.") String descripcion,
        @Pattern(regexp = "^[\\P{Cc}]*$", message = "La ciudad no debe contener caracteres de control.")
        @Size(max = 120, message = "La ciudad no debe superar 120 caracteres.") String ciudad,
        @Pattern(regexp = "^[\\P{Cc}]*$", message = "La calle no debe contener caracteres de control.")
        @Size(max = 200, message = "La calle no debe superar 200 caracteres.") String calle,
        @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos.") String codigoPostal) {

    public CrearSucursalCommand toCommand() {
        return new CrearSucursalCommand(negocioId, nombre, descripcion, ciudad, calle, codigoPostal);
    }
}

