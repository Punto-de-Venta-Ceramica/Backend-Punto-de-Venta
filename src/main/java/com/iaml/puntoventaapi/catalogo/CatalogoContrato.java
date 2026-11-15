package com.iaml.puntoventaapi.catalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CatalogoContrato {
    /** Rango de piezas y porcentaje de un descuento por volumen; cantidadMaxima null = sin tope. */
    record EscalonDescuento(int cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) { }

    /** Escalones ordenados por cantidad mínima; vacío si el producto no tiene descuentos. */
    record ProductoPedido(BigDecimal precio, List<EscalonDescuento> escalones) { }

    Optional<ProductoPedido> productoParaPedido(long productoId, long negocioId);
}
