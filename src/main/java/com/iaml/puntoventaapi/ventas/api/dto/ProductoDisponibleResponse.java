package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.ProductoDisponible;
import com.iaml.puntoventaapi.ventas.domain.RangoDescuentoVenta;
import java.util.List;
import java.math.BigDecimal;

public record ProductoDisponibleResponse(Long productoId, String nombre, BigDecimal precioUnitario, int existencia,
                                         String imagenUrl, Long areaId, String areaNombre,
                                         Long categoriaId, String categoriaNombre, List<RangoDescuentoVenta> descuentos) {

    public static ProductoDisponibleResponse from(ProductoDisponible producto) {
        return new ProductoDisponibleResponse(producto.productoId(), producto.nombre(),
            producto.precioUnitario(), producto.existencia(), producto.imagenUrl(),
            producto.areaId(), producto.areaNombre(), producto.categoriaId(), producto.categoriaNombre(), producto.descuentos());
    }
}
