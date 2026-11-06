package com.iaml.puntoventaapi.autenticacion.domain.port;

import java.util.Optional;

public interface RolRepositorioPort {

    Optional<Long> buscarIdPorNombre(String nombre);
}
