package com.iaml.puntoventaapi.catalogo.domain;

/** Descuento junto con los productos, categorías y áreas a los que está asignado. */
public record DescuentoConAsignaciones(Descuento descuento, AsignacionesDescuento asignaciones) {
}
