package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByCategoria_Id(Long categoriaId);

    boolean existsByCategoria_Area_Id(Long areaId);
}
