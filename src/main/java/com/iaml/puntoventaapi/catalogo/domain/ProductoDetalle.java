package com.iaml.puntoventaapi.catalogo.domain;

/** Producto junto con su descuento activo (null si no tiene). */
public record ProductoDetalle(Producto producto, Descuento descuento) {
}
