package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    // Producto -> categoría -> área -> negocio: la propiedad del negocio se resuelve por esa cadena.
    @Query("""
        select p
        from ProductoEntity p
        join p.categoria c
        join c.area a
        join a.negocio n
        join n.usuarios u
        where p.id = :id and u.id = :usuarioId and p.activo = true and n.activo = true
          and c.activo = true and a.activo = true
        """)
    Optional<ProductoEntity> buscarActivoDelUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("""
        select p
        from ProductoEntity p
        join p.categoria c
        join c.area a
        where a.negocio.id = :negocioId and p.activo = true
          and c.activo = true and a.activo = true and a.negocio.activo = true
        order by p.nombre, p.id
        """)
    List<ProductoEntity> listarActivosPorNegocio(@Param("negocioId") Long negocioId);

    /** Texto ya en minúsculas y con comodines (%texto%); los demás filtros null no se aplican. */
    /** Igual que buscarActivoDelUsuario, pero trae categoría y área en la misma consulta. */
    @Query("""
        select p
        from ProductoEntity p
        join fetch p.categoria c
        join fetch c.area a
        join a.negocio n
        join n.usuarios u
        where p.id = :id and u.id = :usuarioId and p.activo = true and n.activo = true
          and c.activo = true and a.activo = true
        """)
    Optional<ProductoEntity> buscarActivoConClasificacion(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("""
        select p
        from ProductoEntity p
        join fetch p.categoria c
        join fetch c.area a
        join a.negocio n
        where p.id = :id and n.id = :negocioId and p.activo = true and n.activo = true
          and c.activo = true and a.activo = true
        """)
    Optional<ProductoEntity> buscarActivoConClasificacionEnNegocio(@Param("id") Long id,
                                                                   @Param("negocioId") Long negocioId);

    @Query(value = """
        select p
        from ProductoEntity p
        join p.categoria c
        join c.area a
        where a.negocio.id = :negocioId and p.activo = true
          and c.activo = true and a.activo = true and a.negocio.activo = true
          and (:areaId is null or a.id = :areaId)
          and (:categoriaId is null or c.id = :categoriaId)
          and lower(p.nombre) like :texto escape '\\'
          and (:sucursalId is null or exists (
                select 1 from InventarioEntity i where i.producto = p and i.sucursal.id = :sucursalId and i.activo = true))
          and (:precioMinimo is null or p.precio >= :precioMinimo)
          and (:precioMaximo is null or p.precio <= :precioMaximo)
        order by p.nombre, p.id
        """, countQuery = """
        select count(p)
        from ProductoEntity p
        join p.categoria c
        join c.area a
        where a.negocio.id = :negocioId and p.activo = true
          and c.activo = true and a.activo = true and a.negocio.activo = true
          and (:areaId is null or a.id = :areaId)
          and (:categoriaId is null or c.id = :categoriaId)
          and lower(p.nombre) like :texto escape '\\'
          and (:sucursalId is null or exists (
                select 1 from InventarioEntity i where i.producto = p and i.sucursal.id = :sucursalId and i.activo = true))
          and (:precioMinimo is null or p.precio >= :precioMinimo)
          and (:precioMaximo is null or p.precio <= :precioMaximo)
        """)
    Page<ProductoEntity> buscarActivosPorNegocio(@Param("negocioId") Long negocioId,
                                                 @Param("areaId") Long areaId,
                                                 @Param("categoriaId") Long categoriaId,
                                                 @Param("texto") String texto,
                                                 @Param("sucursalId") Long sucursalId,
                                                 @Param("precioMinimo") BigDecimal precioMinimo,
                                                 @Param("precioMaximo") BigDecimal precioMaximo,
                                                 Pageable pageable);

    @Query("select c.id, c.nombre, a.id, a.nombre from CategoriaEntity c join c.area a where c.id = :categoriaId")
    List<Object[]> clasificacionDeCategoria(@Param("categoriaId") Long categoriaId);

    boolean existsByCategoria_Id(Long categoriaId);

    boolean existsByCategoria_Area_Id(Long areaId);

    @Query("""
        select p from ProductoEntity p join p.categoria c join c.area a
        where p.id = :productoId and a.negocio.id = :negocioId and p.activo = true
          and c.activo = true and a.activo = true and a.negocio.activo = true
        """)
    Optional<ProductoEntity> productoParaPedido(@Param("productoId") Long productoId, @Param("negocioId") Long negocioId);
}
