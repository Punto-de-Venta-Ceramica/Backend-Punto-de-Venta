package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepositorioPort {

    /** Id del negocio al que pertenece la categoría (categoría -> área -> negocio). */
    Optional<Long> buscarNegocioId(Long categoriaId);

    /** Reserva el área durante la asignación de un producto a una categoría activa. */
    Optional<Long> buscarNegocioIdParaActualizar(Long categoriaId);

    Categoria guardar(Categoria categoria, Long negocioId);

    Optional<Categoria> buscarPorId(Long categoriaId, Long areaId, Long negocioId);

    List<Categoria> listarPorArea(Long areaId, Long negocioId);

    /** Categorías activas de todas las áreas activas del negocio, agrupadas por área. */
    List<Categoria> listarPorNegocio(Long negocioId);
}
