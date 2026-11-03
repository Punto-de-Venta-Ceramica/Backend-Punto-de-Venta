package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    Optional<CategoriaEntity> findByIdAndArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrue(
        Long categoriaId, Long areaId, Long negocioId);

    List<CategoriaEntity> findByArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrueOrderByNombreAscIdAsc(
        Long areaId, Long negocioId);
}
