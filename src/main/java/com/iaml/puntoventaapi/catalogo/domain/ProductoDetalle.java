package com.iaml.puntoventaapi.catalogo.domain;

import java.util.Comparator;
import java.util.List;

/**
 * Producto con sus descuentos y su clasificación (null si no se resolvió).
 * {@code escalones}: descuentos por cantidad vigentes, ordenados por cantidad mínima (los que se muestran).
 * {@code descuento}: el de menor id entre ellos (null si no tiene); se conserva para el formulario de
 * producto que aún maneja un descuento único.
 */
public record ProductoDetalle(Producto producto, Descuento descuento, ClasificacionProducto clasificacion,
                              List<Descuento> escalones) {

    public ProductoDetalle {
        escalones = escalones == null ? List.of() : List.copyOf(escalones);
    }

    public ProductoDetalle(Producto producto, Descuento descuento, ClasificacionProducto clasificacion) {
        this(producto, descuento, clasificacion, descuento == null ? List.of() : List.of(descuento));
    }

    public ProductoDetalle(Producto producto, Descuento descuento) {
        this(producto, descuento, null);
    }

    /** Detalle a partir de los escalones vigentes: el descuento único es el de menor id. */
    public static ProductoDetalle conEscalones(Producto producto, ClasificacionProducto clasificacion,
                                               List<Descuento> escalones) {
        Descuento primero = escalones.stream().min(Comparator.comparing(Descuento::getId)).orElse(null);
        return new ProductoDetalle(producto, primero, clasificacion, escalones);
    }
}
