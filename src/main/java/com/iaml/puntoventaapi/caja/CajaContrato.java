package com.iaml.puntoventaapi.caja;

import java.util.Optional;

public interface CajaContrato {
    Optional<Long> obtenerCorteAbiertoId(Long sucursalId);
    boolean hayCorteAbierto(Long sucursalId);

    /** Mantiene bloqueado el mismo corte que usa el cierre hasta que termine la transacción. */
    Optional<CorteAbierto> bloquearCorteAbierto(Long sucursalId);

    record CorteAbierto(Long cajaId, Long corteId) { }
}
