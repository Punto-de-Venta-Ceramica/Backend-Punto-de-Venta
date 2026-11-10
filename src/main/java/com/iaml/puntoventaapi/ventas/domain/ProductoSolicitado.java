package com.iaml.puntoventaapi.ventas.domain;

import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import java.util.Map;

public record ProductoSolicitado(Long productoId, int cantidad) {

    public ProductoSolicitado {
        if (productoId == null || productoId <= 0) {
            throw new DatosVentaInvalidosException(Map.of("productos", "Selecciona productos con identificadores válidos."));
        }
        if (cantidad <= 0) {
            throw new DatosVentaInvalidosException(Map.of("productos", "La cantidad de cada producto debe ser mayor que cero."));
        }
    }
}
