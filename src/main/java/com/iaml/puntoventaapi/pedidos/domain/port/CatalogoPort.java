package com.iaml.puntoventaapi.pedidos.domain.port;

import java.math.BigDecimal;

/** Obtiene datos del catálogo para la sucursal; nunca recibe precios del cliente HTTP. */
public interface CatalogoPort {
    record Producto(BigDecimal precio, Integer cantidadMinima, Integer cantidadMaxima, BigDecimal descuento) { }
    Producto consultar(long sucursalId, long usuarioId, long productoId);
}
