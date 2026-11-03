package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepositorioPort {

    Categoria guardar(Categoria categoria, Long negocioId);

    Optional<Categoria> buscarPorId(Long categoriaId, Long areaId, Long negocioId);

    List<Categoria> listarPorArea(Long areaId, Long negocioId);
}
