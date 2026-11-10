package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.ProductoDisponible;
import java.math.BigDecimal;

public record ProductoDisponibleResponse(Long productoId, String nombre, BigDecimal precioUnitario, int existencia) {

    public static ProductoDisponibleResponse from(ProductoDisponible producto) {
        return new ProductoDisponibleResponse(producto.productoId(), producto.nombre(),
            producto.precioUnitario(), producto.existencia());
    }
}
