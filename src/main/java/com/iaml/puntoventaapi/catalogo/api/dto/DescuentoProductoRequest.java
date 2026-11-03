package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.application.DescuentoProductoCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Descuento por volumen: aplica de cantidadMinima a cantidadMaxima (sin tope si es null). */
public record DescuentoProductoRequest(
        @NotNull(message = "La cantidad mínima del descuento es obligatoria.")
        @Min(value = 1, message = "La cantidad mínima del descuento debe ser al menos 1.") Integer cantidadMinima,
        @Min(value = 1, message = "La cantidad máxima del descuento debe ser al menos 1.") Integer cantidadMaxima,
        @NotNull(message = "El porcentaje de descuento es obligatorio.")
        @DecimalMin(value = "0.01", message = "El porcentaje de descuento debe ser mayor a 0.")
        @DecimalMax(value = "100.00", message = "El porcentaje de descuento no puede superar 100.")
        @Digits(integer = 3, fraction = 2, message = "El porcentaje admite máximo 2 decimales.") BigDecimal porcentaje) {

    @JsonIgnore
    @AssertTrue(message = "La cantidad máxima no puede ser menor que la mínima.")
    public boolean isRangoValido() {
        return cantidadMinima == null || cantidadMaxima == null || cantidadMaxima >= cantidadMinima;
    }

    public DescuentoProductoCommand toCommand() {
        return new DescuentoProductoCommand(cantidadMinima, cantidadMaxima, porcentaje);
    }
}
