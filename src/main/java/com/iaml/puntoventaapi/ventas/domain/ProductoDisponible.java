package com.iaml.puntoventaapi.ventas.domain;

import java.math.BigDecimal;
import java.util.List;

public record ProductoDisponible(Long productoId, String nombre, BigDecimal precioUnitario, int existencia,
                                 String imagenUrl, Long areaId, String areaNombre,
                                 Long categoriaId, String categoriaNombre, List<RangoDescuentoVenta> descuentos) {

    public ProductoDisponible conDescuentos(List<RangoDescuentoVenta> rangos) {
        return new ProductoDisponible(productoId, nombre, precioUnitario, existencia, imagenUrl,
            areaId, areaNombre, categoriaId, categoriaNombre, rangos);
    }
}
