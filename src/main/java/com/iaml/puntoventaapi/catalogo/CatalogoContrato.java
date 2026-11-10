package com.iaml.puntoventaapi.catalogo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface CatalogoContrato {
    record ProductoPedido(BigDecimal precio, Integer cantidadMinima, Integer cantidadMaxima, BigDecimal descuento) { }
    Optional<ProductoPedido> productoParaPedido(long productoId, long negocioId);

    /** Devuelve solo productos activos del negocio y calcula el descuento para cada cantidad. */
    Map<Long, ProductoParaVenta> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto);
}
