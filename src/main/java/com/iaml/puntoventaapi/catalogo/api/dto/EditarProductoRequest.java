package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.application.EditarProductoCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Reemplazo completo de los datos editables. Si descuento es null, el producto queda sin descuento. */
public record EditarProductoRequest(
        @NotBlank(message = "El nombre es obligatorio y no puede contener solo espacios.")
        @Pattern(regexp = "^[\\P{Cc}]*$", message = "El nombre no debe contener saltos de línea ni caracteres de control.")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres.") String nombre,
        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "El precio admite máximo 10 enteros y 2 decimales.") BigDecimal precio,
        @NotNull(message = "El id de la categoría es obligatorio.")
        @Positive(message = "El id de la categoría debe ser positivo.") Long categoriaId,
        @Valid DescuentoProductoRequest descuento) {

    public EditarProductoCommand toCommand(Long id) {
        return new EditarProductoCommand(id, nombre, precio, categoriaId,
            descuento == null ? null : descuento.toCommand());
    }
}
