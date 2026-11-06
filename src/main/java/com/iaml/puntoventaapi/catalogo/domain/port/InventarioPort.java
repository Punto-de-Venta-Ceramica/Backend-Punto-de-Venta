package com.iaml.puntoventaapi.catalogo.domain.port;

import java.util.Map;

public interface InventarioPort {

    /** id de sucursal -> existencia inicial del producto recién creado. */
    void registrarExistenciasIniciales(Long productoId, Map<Long, Integer> existenciaPorSucursal);
}
