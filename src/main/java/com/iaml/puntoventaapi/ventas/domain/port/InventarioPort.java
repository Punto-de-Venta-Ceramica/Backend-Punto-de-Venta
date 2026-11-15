package com.iaml.puntoventaapi.ventas.domain.port;

import java.util.Map;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import com.iaml.puntoventaapi.ventas.domain.ProductoDisponible;

public interface InventarioPort {
    Map<Long, Integer> consultarExistencias(Long negocioId, Long sucursalId, Long usuarioId);

    Pagina<ProductoDisponible> listarProductos(Long negocioId, Long sucursalId, String buscar, int pagina, int tamanio);

    void descontarParaVenta(Long sucursalId, Map<Long, Integer> cantidades);
}
