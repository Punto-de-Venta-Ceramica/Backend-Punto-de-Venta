package com.iaml.puntoventaapi.catalogo;

import java.math.BigDecimal;
import java.util.Optional;

public interface CatalogoContrato {
    record ProductoPedido(BigDecimal precio, Integer cantidadMinima, Integer cantidadMaxima, BigDecimal descuento) { }
    Optional<ProductoPedido> productoParaPedido(long productoId, long negocioId);
}
