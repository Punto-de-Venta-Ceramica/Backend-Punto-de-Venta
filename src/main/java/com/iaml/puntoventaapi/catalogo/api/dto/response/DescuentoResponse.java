package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DescuentoConAsignaciones;
import java.math.BigDecimal;
import java.util.List;

/** Descuento con los identificadores de los productos, categorías y áreas a los que está asignado. */
public record DescuentoResponse(Long id, Long negocioId, String nombre, Integer cantidadMinima,
                                Integer cantidadMaxima, BigDecimal porcentaje, boolean activo,
                                List<Long> productoIds, List<Long> categoriaIds, List<Long> areaIds) {

    public static DescuentoResponse from(DescuentoConAsignaciones detalle) {
        Descuento d = detalle.descuento();
        return new DescuentoResponse(d.getId(), d.getNegocioId(), d.getNombre(), d.getCantidadMinima(),
            d.getCantidadMaxima(), d.getPorcentaje(), d.isActivo(),
            detalle.asignaciones().productoIds().stream().sorted().toList(),
            detalle.asignaciones().categoriaIds().stream().sorted().toList(),
            detalle.asignaciones().areaIds().stream().sorted().toList());
    }
}
