package com.iaml.puntoventaapi.catalogo.domain.port;

import java.util.List;
import java.util.Optional;

public interface SucursalPort {

    List<Long> idsParaRegistrarProductos(Long negocioId, Long usuarioId);
    Optional<Long> negocioAutorizado(Long sucursalId, Long usuarioId);
}
