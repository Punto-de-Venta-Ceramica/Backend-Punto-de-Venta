package com.iaml.puntoventaapi.ventas.domain.port;

import java.util.Optional;

public interface SucursalPort {
    Optional<Long> obtenerNegocioIdAccesible(Long sucursalId, Long usuarioId);
}
