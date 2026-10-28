package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SucursalJpaRepository extends JpaRepository<SucursalEntity, Long> {

    Optional<SucursalEntity> findByIdAndNegocio_Usuarios_IdAndNegocio_ActivoTrue(Long id, Long usuarioId);

    List<SucursalEntity> findByNegocio_IdAndNegocio_Usuarios_IdAndNegocio_ActivoTrueOrderByIdAsc(Long negocioId, Long usuarioId);
}

