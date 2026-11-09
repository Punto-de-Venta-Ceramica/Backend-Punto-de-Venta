package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface DescuentoRepositorioPort {

    /** Inserta si no tiene id; si lo tiene, actualiza nombre, rango y porcentaje. */
    Descuento guardar(Descuento descuento);

    /** Enlaza el descuento con el producto (tabla descuento_producto). */
    void vincularAProducto(Long descuentoId, Long productoId);

    /**
     * Descuento activo que aplica al producto: el asignado directamente al producto; si no hay,
     * el de su categoría; si no, el de su área. Dentro del mismo nivel gana el de menor id.
     */
    Optional<Descuento> buscarActivoPorProducto(Long productoId);

    /** Igual que {@link #buscarActivoPorProducto} para varios productos (los que no tienen no aparecen). */
    Map<Long, Descuento> buscarActivosPorProductos(Collection<Long> productoIds);

    /**
     * Descuento activo propio del producto: el que solo está asignado a ese producto (no a otros
     * productos, ni a categorías o áreas). Es el único que se edita en sitio al guardar un producto con un
     * descuento único (compatibilidad).
     */
    Optional<Descuento> buscarPropioActivoDeProducto(Long productoId);
}
