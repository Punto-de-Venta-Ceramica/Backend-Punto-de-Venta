package com.iaml.puntoventaapi.catalogo.domain;

import java.util.Set;

/** Identificadores de los productos, categorías y áreas a los que está asignado un descuento. */
public record AsignacionesDescuento(Set<Long> productoIds, Set<Long> categoriaIds, Set<Long> areaIds) {

    public AsignacionesDescuento {
        productoIds = Set.copyOf(productoIds);
        categoriaIds = Set.copyOf(categoriaIds);
        areaIds = Set.copyOf(areaIds);
    }

    public static AsignacionesDescuento vacia() {
        return new AsignacionesDescuento(Set.of(), Set.of(), Set.of());
    }
}
