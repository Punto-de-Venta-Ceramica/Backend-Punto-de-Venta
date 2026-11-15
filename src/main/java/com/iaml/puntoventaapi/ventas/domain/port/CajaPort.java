package com.iaml.puntoventaapi.ventas.domain.port;

import java.util.Optional;

public interface CajaPort {
    Optional<CorteAbierto> bloquearCorteAbierto(Long sucursalId);

    record CorteAbierto(Long cajaId, Long corteId) { }
}
