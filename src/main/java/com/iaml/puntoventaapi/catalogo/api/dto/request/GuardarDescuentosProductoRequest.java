package com.iaml.puntoventaapi.catalogo.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iaml.puntoventaapi.catalogo.application.LineaDescuentoCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estado final de los descuentos propios de un producto: los ids de {@code eliminar} se borran y cada
 * línea se crea (sin id) o se actualiza (con id).
 */
public record GuardarDescuentosProductoRequest(
        @Size(max = 50, message = "No se pueden eliminar más de 50 descuentos a la vez.")
        List<@NotNull @Positive Long> eliminar,
        @Size(max = 50, message = "Un producto no puede tener más de 50 descuentos.")
        List<@NotNull @Valid Linea> lineas) {

    public record Linea(
            @Positive(message = "El id del descuento debe ser positivo.") Long id,
            @NotBlank(message = "El nombre del descuento es obligatorio.")
            @Size(max = 120, message = "El nombre del descuento no debe superar 120 caracteres.") String nombre,
            @NotNull(message = "La cantidad mínima del descuento es obligatoria.")
            @Min(value = 1, message = "La cantidad mínima del descuento debe ser al menos 1.") Integer cantidadMinima,
            @Min(value = 1, message = "La cantidad máxima del descuento debe ser al menos 1.") Integer cantidadMaxima,
            @NotNull(message = "El porcentaje de descuento es obligatorio.")
            @DecimalMin(value = "0.01", message = "El porcentaje de descuento debe ser mayor a 0.")
            @DecimalMax(value = "100.00", message = "El porcentaje de descuento no puede superar 100.")
            @Digits(integer = 3, fraction = 2, message = "El porcentaje admite máximo 2 decimales.") BigDecimal porcentaje,
            Boolean activo) {

        @JsonIgnore
        @AssertTrue(message = "La cantidad máxima no puede ser menor que la mínima.")
        public boolean isRangoValido() {
            return cantidadMinima == null || cantidadMaxima == null || cantidadMaxima >= cantidadMinima;
        }

        /** Sin indicar el estado, el descuento queda activo. */
        LineaDescuentoCommand toCommand() {
            return new LineaDescuentoCommand(id, nombre, cantidadMinima, cantidadMaxima, porcentaje,
                !Boolean.FALSE.equals(activo));
        }
    }

    public List<Long> idsAEliminar() {
        return eliminar == null ? List.of() : eliminar;
    }

    public List<LineaDescuentoCommand> toCommands() {
        return lineas == null ? List.of() : lineas.stream().map(Linea::toCommand).toList();
    }
}
