package com.iaml.puntoventaapi.catalogo;

/** Producto "Artículo artesanal" de cada negocio: se cobra con precio y nombre capturados en la venta. */
public interface ProductoLibreContrato {

    String NOMBRE = "Artículo artesanal";

    record ProductoLibre(Long productoId, String nombre) { }

    /** Devuelve el producto del negocio y lo crea la primera vez; requiere una transacción de escritura. */
    ProductoLibre obtenerOCrear(Long negocioId);
}
