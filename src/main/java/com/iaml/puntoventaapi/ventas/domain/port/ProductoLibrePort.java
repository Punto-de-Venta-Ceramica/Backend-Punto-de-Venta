package com.iaml.puntoventaapi.ventas.domain.port;

public interface ProductoLibrePort {

    record ProductoLibre(Long productoId, String nombre) { }

    /** Producto "Artículo artesanal" del negocio; lo crea la primera vez. */
    ProductoLibre obtenerOCrear(Long negocioId);
}
