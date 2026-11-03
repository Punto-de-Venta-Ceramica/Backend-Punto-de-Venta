package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaJpaRepository extends JpaRepository<AreaEntity, Long> {

    Optional<AreaEntity> findByIdAndNegocio_IdAndNegocio_ActivoTrue(Long areaId, Long negocioId);

    List<AreaEntity> findByNegocio_IdAndNegocio_ActivoTrueOrderByNombreAscIdAsc(Long negocioId);
}
