package com.iaml.puntoventaapi.sucursal;

import java.util.List;
import java.util.Optional;

public interface SucursalContrato {

    /** Ids de las sucursales del negocio, si el negocio está activo y pertenece al usuario; si no, lista vacía. */
    List<Long> idsPorNegocio(Long negocioId, Long usuarioId);

    boolean perteneceANegocio(Long sucursalId, Long negocioId);

    Optional<String> obtenerNombrePorId(Long sucursalId);
}
