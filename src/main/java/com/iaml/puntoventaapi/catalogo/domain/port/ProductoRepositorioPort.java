package com.iaml.puntoventaapi.catalogo.domain.port;

import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositorioPort {

    /** Inserta si no tiene id; si lo tiene, actualiza nombre, precio, categoría y estado. */
    Producto guardar(Producto producto);

    /** Producto activo de un negocio activo del usuario. */
    Optional<Producto> buscarPorId(Long id, Long usuarioId);

    /** Productos activos del negocio, ordenados por nombre. */
    List<Producto> listarPorNegocio(Long negocioId);

    /** Página de productos activos del negocio (pagina desde 0), ordenados por nombre, con filtros opcionales. */
    Pagina<Producto> listarPagina(Long negocioId, FiltroProductos filtro, int pagina, int tamanio);

    /** Categoría y área de la categoría indicada. */
    Optional<ClasificacionProducto> buscarClasificacion(Long categoriaId);

    /** Incluye productos activos y dados de baja, pues ambos conservan la relación. */
    boolean existenPorCategoria(Long categoriaId);

    boolean existenPorArea(Long areaId);
}
