package com.iaml.puntoventaapi.catalogo.domain;

/** Producto con su descuento activo (null si no tiene) y su clasificación (null si no se resolvió). */
public record ProductoDetalle(Producto producto, Descuento descuento, ClasificacionProducto clasificacion) {

    public ProductoDetalle(Producto producto, Descuento descuento) {
        this(producto, descuento, null);
    }
}
