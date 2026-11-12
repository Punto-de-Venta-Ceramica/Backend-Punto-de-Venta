package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Gestión de descuentos de un negocio y de sus asignaciones a productos, categorías y áreas. */
public interface DescuentoGestionPort {

    /** Inserta si no tiene id; si lo tiene, actualiza nombre, rango, porcentaje y estado. */
    Descuento guardar(Descuento descuento);

    Optional<Descuento> buscarPorId(Long id, Long negocioId);

    /** Todos los descuentos del negocio, activos e inactivos, ordenados por id. */
    List<Descuento> listarPorNegocio(Long negocioId);

    /** Elimina el descuento y todas sus asignaciones. */
    void eliminar(Long descuentoId);

    AsignacionesDescuento asignaciones(Long descuentoId);

    /** Asignaciones de cada descuento indicado (los que no tienen ninguna también aparecen, vacías). */
    Map<Long, AsignacionesDescuento> asignacionesDe(Collection<Long> descuentoIds);

    /** El destino existe, está activo y pertenece al negocio. */
    boolean destinoPerteneceAlNegocio(DestinoDescuento destino, Long destinoId, Long negocioId);

    /**
     * Descuentos activos asignados directamente a cada producto, categoría o área indicado, en una sola
     * consulta. Los destinos sin descuentos no aparecen.
     */
    Map<Long, List<Descuento>> activosAsignadosA(DestinoDescuento destino, Collection<Long> destinoIds);

    /** Idempotente: asignar dos veces el mismo destino no duplica el vínculo. */
    void asignar(Long descuentoId, DestinoDescuento destino, Long destinoId);

    /** Idempotente: quitar un destino que no estaba asignado no es un error. */
    void quitar(Long descuentoId, DestinoDescuento destino, Long destinoId);
}
