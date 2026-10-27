package com.iaml.puntoventaapi.negocios.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NegocioJpaRepository extends JpaRepository<NegocioEntity, Long> {

    List<NegocioResumen> findAllByUsuarios_IdAndActivoTrueOrderByNombreAscIdAsc(Long usuarioId);

    Optional<NegocioEntity> findByIdAndUsuarios_IdAndActivoTrue(Long negocioId, Long usuarioId);

    boolean existsByIdAndUsuarios_IdAndActivoTrue(Long negocioId, Long usuarioId);
}
