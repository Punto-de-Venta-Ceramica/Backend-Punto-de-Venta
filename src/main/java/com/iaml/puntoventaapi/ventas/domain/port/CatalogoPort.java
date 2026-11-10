package com.iaml.puntoventaapi.ventas.domain.port;

import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
import java.util.Map;

public interface CatalogoPort {
    Map<Long, ProductoCotizado> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto);
}
