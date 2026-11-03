package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    @Query("select a.negocio.id from CategoriaEntity c join c.area a where c.id = :categoriaId")
    Optional<Long> buscarNegocioId(@Param("categoriaId") Long categoriaId);

    Optional<CategoriaEntity> findByIdAndArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrue(
        Long categoriaId, Long areaId, Long negocioId);

    List<CategoriaEntity> findByArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrueOrderByNombreAscIdAsc(
        Long areaId, Long negocioId);
}
