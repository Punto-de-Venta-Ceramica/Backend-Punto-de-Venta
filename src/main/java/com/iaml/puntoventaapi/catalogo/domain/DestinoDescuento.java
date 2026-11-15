package com.iaml.puntoventaapi.catalogo.domain;

/**
 * A qué se puede asignar un descuento. Al resolver el descuento de un producto la prioridad es
 * PRODUCTO, luego CATEGORIA y por último AREA; dentro del mismo nivel gana el de menor id.
 */
public enum DestinoDescuento {
    PRODUCTO,
    CATEGORIA,
    AREA
}
