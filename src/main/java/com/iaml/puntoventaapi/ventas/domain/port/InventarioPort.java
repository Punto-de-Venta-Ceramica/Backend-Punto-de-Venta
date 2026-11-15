package com.iaml.puntoventaapi.ventas.domain.port;

import java.util.Map;

public interface InventarioPort {
    Map<Long, Integer> consultarExistencias(Long negocioId, Long sucursalId, Long usuarioId);

    void descontarParaVenta(Long sucursalId, Map<Long, Integer> cantidades);
}
