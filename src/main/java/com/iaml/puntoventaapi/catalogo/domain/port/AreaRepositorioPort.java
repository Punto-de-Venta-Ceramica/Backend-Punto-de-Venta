package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.Area;
import java.util.List;
import java.util.Optional;

public interface AreaRepositorioPort {

    Area guardar(Area area);

    Optional<Area> buscarPorId(Long areaId, Long negocioId);

    /** Serializa los cambios del área, sus categorías y las asignaciones de productos. */
    Optional<Area> buscarPorIdParaActualizar(Long areaId, Long negocioId);

    List<Area> listarPorNegocio(Long negocioId);
}
