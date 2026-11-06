package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    @Query("""
        select a.negocio.id from CategoriaEntity c join c.area a
        where c.id = :categoriaId and c.activo = true and a.activo = true and a.negocio.activo = true
        """)
    Optional<Long> buscarNegocioId(@Param("categoriaId") Long categoriaId);

    @Query("""
        select c.area.id from CategoriaEntity c
        where c.id = :categoriaId and c.activo = true and c.area.activo = true
          and c.area.negocio.activo = true
        """)
    Optional<Long> buscarAreaId(@Param("categoriaId") Long categoriaId);

    @Query("""
        select c from CategoriaEntity c
        where c.id = :categoriaId and c.area.id = :areaId and c.area.negocio.id = :negocioId
          and c.activo = true and c.area.activo = true and c.area.negocio.activo = true
        """)
    Optional<CategoriaEntity> buscarActivaPorId(@Param("categoriaId") Long categoriaId,
        @Param("areaId") Long areaId, @Param("negocioId") Long negocioId);

    @Query("""
        select c from CategoriaEntity c
        where c.area.id = :areaId and c.area.negocio.id = :negocioId
          and c.activo = true and c.area.activo = true and c.area.negocio.activo = true
        order by c.nombre, c.id
        """)
    List<CategoriaEntity> listarActivasPorArea(@Param("areaId") Long areaId, @Param("negocioId") Long negocioId);
}
