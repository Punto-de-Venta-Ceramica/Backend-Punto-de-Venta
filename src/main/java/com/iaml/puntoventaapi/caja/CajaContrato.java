package com.iaml.puntoventaapi.caja;

import java.util.Optional;

public interface CajaContrato {
    Optional<Long> obtenerCorteAbiertoId(Long sucursalId);
    boolean hayCorteAbierto(Long sucursalId);
}
