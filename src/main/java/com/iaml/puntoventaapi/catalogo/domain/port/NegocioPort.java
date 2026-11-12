package com.iaml.puntoventaapi.catalogo.domain.port;

import java.util.Optional;

public interface NegocioPort {

    boolean perteneceAUsuario(Long negocioId, Long usuarioId);

    /**
     * Negocio de la sucursal si el usuario es su propietario o un empleado asignado a ella. Permite al
     * empleado consultar (sin modificar) el catálogo de su sucursal.
     */
    default Optional<Long> negocioDeSucursal(Long sucursalId, Long usuarioId) {
        return Optional.empty();
    }

    /** Propietario del negocio, o empleado asignado a una sucursal suya: puede consultar el catálogo. */
    default boolean puedeConsultar(Long negocioId, Long sucursalId, Long usuarioId) {
        return perteneceAUsuario(negocioId, usuarioId)
            || (sucursalId != null && negocioDeSucursal(sucursalId, usuarioId).filter(negocioId::equals).isPresent());
    }
}
