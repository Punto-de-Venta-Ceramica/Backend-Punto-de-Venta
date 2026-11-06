package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import java.math.BigDecimal;

public record ProductoResponse(Long id, String nombre, BigDecimal precio, Long categoriaId,
                               DescuentoResponse descuento) {

    public record DescuentoResponse(Integer cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) {

        static DescuentoResponse from(Descuento descuento) {
            return descuento == null ? null : new DescuentoResponse(descuento.getCantidadMinima(),
                descuento.getCantidadMaxima(), descuento.getPorcentaje());
        }
    }

    public static ProductoResponse from(ProductoDetalle detalle) {
        Producto producto = detalle.producto();
        return new ProductoResponse(producto.getId(), producto.getNombre(), producto.getPrecio(),
            producto.getCategoriaId(), DescuentoResponse.from(detalle.descuento()));
    }
}
