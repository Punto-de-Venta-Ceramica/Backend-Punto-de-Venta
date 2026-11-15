package com.iaml.puntoventaapi.catalogo.domain.port;

import java.util.List;

public interface SucursalPort {

    List<Long> idsPorNegocio(Long negocioId, Long usuarioId);
}
