package com.iaml.puntoventaapi.ventas.domain.port;

import java.util.Optional;

/** Datos del negocio que necesita el ticket de venta. */
public interface NegocioPort {
    Optional<String> eslogan(Long negocioId);
}
