package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle de un producto: incluye imagen, categoría y área (null si no se pudieron resolver) y sus
 * descuentos por cantidad vigentes, ordenados por cantidad mínima (vacío si no tiene).
 */
public record ProductoResponse(Long id, String nombre, BigDecimal precio, Long categoriaId, String categoriaNombre,
                               Long areaId, String areaNombre,
                               String imagenUrl, String imagenPublicId,
                               List<DescuentoResponse> descuentos) {

    public record DescuentoResponse(Integer cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) {

        static DescuentoResponse from(Descuento descuento) {
            return new DescuentoResponse(descuento.getCantidadMinima(), descuento.getCantidadMaxima(),
                descuento.getPorcentaje());
        }
    }

    public static ProductoResponse from(ProductoDetalle detalle) {
        Producto producto = detalle.producto();
        ClasificacionProducto clasificacion = detalle.clasificacion();
        return new ProductoResponse(producto.getId(), producto.getNombre(), producto.getPrecio(),
            producto.getCategoriaId(), clasificacion == null ? null : clasificacion.categoriaNombre(),
            clasificacion == null ? null : clasificacion.areaId(),
            clasificacion == null ? null : clasificacion.areaNombre(),
            producto.getImagenUrl(), producto.getImagenPublicId(),
            detalle.escalones().stream().map(DescuentoResponse::from).toList());
    }
}
