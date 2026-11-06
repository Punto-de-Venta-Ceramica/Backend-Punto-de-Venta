package com.iaml.puntoventaapi.inventario.domain;

/** Fila del panel global de inventario: existencia de un producto en una sucursal. */
public record ExistenciaProducto(Long inventarioId, Long productoId, String productoNombre,
                                 Long sucursalId, String sucursalNombre, int existencia) {
}
