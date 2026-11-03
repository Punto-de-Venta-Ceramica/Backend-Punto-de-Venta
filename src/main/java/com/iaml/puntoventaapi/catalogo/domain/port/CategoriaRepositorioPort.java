package com.iaml.puntoventaapi.catalogo.domain.port;

import java.util.Optional;

public interface CategoriaRepositorioPort {

    /** Id del negocio al que pertenece la categoría (categoría -> área -> negocio). */
    Optional<Long> buscarNegocioId(Long categoriaId);
}
