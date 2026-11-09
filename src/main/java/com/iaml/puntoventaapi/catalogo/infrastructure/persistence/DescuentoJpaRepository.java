package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DescuentoJpaRepository extends JpaRepository<DescuentoEntity, Long> {

    // Las tres consultas "activos..." devuelven filas [idProducto, DescuentoEntity] ordenadas por id de descuento.

    /** Descuentos activos asignados directamente al producto. */
    @Query("""
        select p.id, d
        from DescuentoEntity d
        join d.productos p
        where p.id in :productoIds and d.activo = true
        order by d.id
        """)
    List<Object[]> activosDirectosPorProductos(@Param("productoIds") Collection<Long> productoIds);

    /** Descuentos activos asignados a la categoría del producto. */
    @Query("""
        select p.id, d
        from DescuentoEntity d
        join d.categorias c
        join ProductoEntity p on p.categoria = c
        where p.id in :productoIds and d.activo = true
        order by d.id
        """)
    List<Object[]> activosPorCategoriaDeProductos(@Param("productoIds") Collection<Long> productoIds);

    /** Descuentos activos asignados al área de la categoría del producto. */
    @Query("""
        select p.id, d
        from DescuentoEntity d
        join d.areas a
        join CategoriaEntity c on c.area = a
        join ProductoEntity p on p.categoria = c
        where p.id in :productoIds and d.activo = true
        order by d.id
        """)
    List<Object[]> activosPorAreaDeProductos(@Param("productoIds") Collection<Long> productoIds);

    /** Descuentos cuya única asignación es este producto. */
    @Query("""
        select d
        from DescuentoEntity d
        join d.productos p
        where p.id = :productoId
          and size(d.productos) = 1
          and d.categorias is empty
          and d.areas is empty
        order by d.id
        """)
    List<DescuentoEntity> propiosDeProducto(@Param("productoId") Long productoId);

    Optional<DescuentoEntity> findByIdAndNegocioId(Long id, Long negocioId);

    List<DescuentoEntity> findByNegocioIdOrderById(Long negocioId);

    // Cada fila: [idDescuento, idDestino].
    @Query("select d.id, p.id from DescuentoEntity d join d.productos p where d.id in :ids")
    List<Object[]> productosAsignados(@Param("ids") Collection<Long> ids);

    @Query("select d.id, c.id from DescuentoEntity d join d.categorias c where d.id in :ids")
    List<Object[]> categoriasAsignadas(@Param("ids") Collection<Long> ids);

    @Query("select d.id, a.id from DescuentoEntity d join d.areas a where d.id in :ids")
    List<Object[]> areasAsignadas(@Param("ids") Collection<Long> ids);

    @Query("""
        select count(p) > 0 from ProductoEntity p
        where p.id = :id and p.activo = true and p.categoria.area.negocio.id = :negocioId
        """)
    boolean productoActivoDelNegocio(@Param("id") Long id, @Param("negocioId") Long negocioId);

    @Query("""
        select count(c) > 0 from CategoriaEntity c
        where c.id = :id and c.activo = true and c.area.negocio.id = :negocioId
        """)
    boolean categoriaActivaDelNegocio(@Param("id") Long id, @Param("negocioId") Long negocioId);

    @Query("""
        select count(a) > 0 from AreaEntity a
        where a.id = :id and a.activo = true and a.negocio.id = :negocioId
        """)
    boolean areaActivaDelNegocio(@Param("id") Long id, @Param("negocioId") Long negocioId);
}
