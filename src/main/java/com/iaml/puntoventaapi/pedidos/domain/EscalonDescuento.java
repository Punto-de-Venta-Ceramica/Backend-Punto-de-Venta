package com.iaml.puntoventaapi.pedidos.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/** Descuento por volumen: aplica cuando la cantidad está entre cantidadMinima y cantidadMaxima (null = sin tope). */
public record EscalonDescuento(int cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) {

    public EscalonDescuento {
        Objects.requireNonNull(porcentaje, "El porcentaje es obligatorio.");
        if (cantidadMinima < 1 || (cantidadMaxima != null && cantidadMaxima < cantidadMinima)
                || porcentaje.signum() < 0 || porcentaje.compareTo(new BigDecimal("100")) > 0
                || porcentaje.scale() > 2) {
            throw new IllegalArgumentException("El descuento del catálogo no es válido.");
        }
    }

    public boolean aplicaA(int cantidad) {
        return cantidad >= cantidadMinima && (cantidadMaxima == null || cantidad <= cantidadMaxima);
    }

    /**
     * Escalón en el que cae la cantidad. El catálogo impide rangos traslapados en un mismo destino; si aun así
     * hubiera varios (datos previos), gana el de mayor cantidad mínima, que es el más específico.
     */
    public static Optional<EscalonDescuento> paraCantidad(Collection<EscalonDescuento> escalones, int cantidad) {
        return escalones.stream()
            .filter(e -> e.aplicaA(cantidad))
            .max(Comparator.comparingInt(EscalonDescuento::cantidadMinima));
    }
}
