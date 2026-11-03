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

    Optional<Descuento> buscarActivoPorProducto(Long productoId);

    /** Descuento activo de cada producto indicado (los productos sin descuento no aparecen). */
    Map<Long, Descuento> buscarActivosPorProductos(Collection<Long> productoIds);

    /** Desenlaza el producto de sus descuentos y elimina los que se quedan sin ningún producto. */
    void quitarDeProducto(Long productoId);
}
