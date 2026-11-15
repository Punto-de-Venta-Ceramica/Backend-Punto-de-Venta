package com.iaml.puntoventaapi.negocios;

import java.util.Optional;

public interface NegociosContrato {
    boolean perteneceAUsuario(Long negocioId, Long usuarioId);

    /** Eslogan del negocio activo para el pie de los tickets; vacío si no tiene. */
    Optional<String> eslogan(Long negocioId);
}
