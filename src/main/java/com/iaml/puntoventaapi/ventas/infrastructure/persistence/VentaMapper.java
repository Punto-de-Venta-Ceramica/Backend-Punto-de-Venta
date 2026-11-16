package com.iaml.puntoventaapi.ventas.infrastructure.persistence;

import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import java.util.List;

final class VentaMapper {
    private VentaMapper() { }

    static VentaConfirmada toDomain(Venta entity, List<DetalleVenta> detalles) {
        var sucursal = entity.getCaja().getSucursal();
        var venta = new com.iaml.puntoventaapi.ventas.domain.Venta(
            entity.getUsuario().getId(), sucursal.getNegocio().getId(), sucursal.getId(),
            MetodoPago.valueOf(entity.getMetodoPago()), detalles.stream().map(VentaMapper::toDomain).toList(),
            entity.getMontoRecibido());
        return new VentaConfirmada(entity.getId(), entity.getFolio(), entity.getCaja().getId(), entity.getCorteCaja().getId(),
            entity.getFechaHora(), venta);
    }

    private static com.iaml.puntoventaapi.ventas.domain.DetalleVenta toDomain(DetalleVenta entity) {
        String nombre = entity.getNombreProducto() == null ? entity.getProducto().getNombre() : entity.getNombreProducto();
        return new com.iaml.puntoventaapi.ventas.domain.DetalleVenta(entity.getId().getProductoId(), nombre,
            entity.getCantidad(), entity.getPrecioUnitario(), entity.getDescAplicado());
    }
}
