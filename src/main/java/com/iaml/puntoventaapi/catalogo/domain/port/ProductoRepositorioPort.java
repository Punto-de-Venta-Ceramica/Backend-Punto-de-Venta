package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositorioPort {

    /** Inserta si no tiene id; si lo tiene, actualiza nombre, precio, categoría y estado. */
    Producto guardar(Producto producto);

    /** Producto activo de un negocio activo del usuario. */
    Optional<Producto> buscarPorId(Long id, Long usuarioId);

    /** Productos activos del negocio, ordenados por nombre. */
    List<Producto> listarPorNegocio(Long negocioId);

    /** Incluye productos activos y dados de baja, pues ambos conservan la relación. */
    boolean existenPorCategoria(Long categoriaId);

    boolean existenPorArea(Long areaId);
}
