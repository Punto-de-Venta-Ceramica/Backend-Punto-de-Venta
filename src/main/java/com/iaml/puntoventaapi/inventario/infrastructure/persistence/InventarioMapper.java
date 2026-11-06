package com.iaml.puntoventaapi.inventario.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoEntity;
import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;

public final class InventarioMapper {

    private InventarioMapper() {
    }

    public static Inventario toDomain(InventarioEntity entity) {
        return new Inventario(entity.getId(), entity.getSucursal().getId(), entity.getProducto().getId(),
            entity.getExistencia());
    }

    public static InventarioEntity toEntity(Inventario inventario, SucursalEntity sucursal, ProductoEntity producto) {
        InventarioEntity entity = new InventarioEntity();
        entity.setSucursal(sucursal);
        entity.setProducto(producto);
        entity.setExistencia(inventario.getExistencia());
        return entity;
    }

    /** Fila del panel: id, idProducto, nombreProducto, idSucursal, nombreSucursal, existencia. */
    public static ExistenciaProducto toExistencia(Object[] fila) {
        return new ExistenciaProducto(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue(),
            (String) fila[2], ((Number) fila[3]).longValue(), (String) fila[4], ((Number) fila[5]).intValue());
    }
}
