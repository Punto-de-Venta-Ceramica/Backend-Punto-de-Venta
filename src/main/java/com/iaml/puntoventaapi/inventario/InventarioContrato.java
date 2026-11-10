package com.iaml.puntoventaapi.inventario;

import java.util.Map;

public interface InventarioContrato {

    /**
     * Crea el registro de inventario de un producto recién creado en cada sucursal indicada.
     * Participa en la transacción del llamador.
     *
     * @param existenciaPorSucursal id de sucursal -> existencia inicial (>= 0)
     */
    void registrarExistenciasIniciales(Long productoId, Map<Long, Integer> existenciaPorSucursal);

    /** Consulta disponibilidad sin reservar ni modificar las existencias. */
    Map<Long, Integer> consultarExistencias(Long negocioId, Long sucursalId, Long usuarioId);
}
