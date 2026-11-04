package com.iaml.puntoventaapi.autenticacion.domain.port;

import java.util.List;
import java.util.Optional;

/** Permisos vigentes obtenidos de la fuente de identidad, no del contenido del token. */
public interface AutoridadesVigentesPort {
    record Autoridades(String rol, List<String> permisos) { }
    Optional<Autoridades> buscar(long usuarioId);
}
