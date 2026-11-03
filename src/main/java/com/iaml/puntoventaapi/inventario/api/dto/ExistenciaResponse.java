package com.iaml.puntoventaapi.inventario.api.dto;

import com.iaml.puntoventaapi.inventario.domain.Inventario;

public record ExistenciaResponse(Long id, Long sucursalId, Long productoId, int existencia) {

    public static ExistenciaResponse from(Inventario inventario) {
        return new ExistenciaResponse(inventario.getId(), inventario.getSucursalId(),
            inventario.getProductoId(), inventario.getExistencia());
    }
}
