package com.iaml.puntoventaapi.usuarios.domain.port;

import java.util.Optional;

public interface SucursalPort {

    boolean perteneceANegocio(Long sucursalId, Long negocioId);

    Optional<String> obtenerNombrePorId(Long sucursalId);
}

