package com.iaml.puntoventaapi.ventas.domain.port;

import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
import com.iaml.puntoventaapi.ventas.domain.RangoDescuentoVenta;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CatalogoPort {
    Map<Long, List<RangoDescuentoVenta>> descuentosDisponibles(Collection<Long> productos);

    Map<Long, ProductoCotizado> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto);
}
