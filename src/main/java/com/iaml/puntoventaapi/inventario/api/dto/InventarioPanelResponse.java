package com.iaml.puntoventaapi.inventario.api.dto;

import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;

public record InventarioPanelResponse(Long id, Long productoId, String productoNombre,
                                      Long sucursalId, String sucursalNombre, int existencia) {

    public static InventarioPanelResponse from(ExistenciaProducto fila) {
        return new InventarioPanelResponse(fila.inventarioId(), fila.productoId(), fila.productoNombre(),
            fila.sucursalId(), fila.sucursalNombre(), fila.existencia());
    }
}
