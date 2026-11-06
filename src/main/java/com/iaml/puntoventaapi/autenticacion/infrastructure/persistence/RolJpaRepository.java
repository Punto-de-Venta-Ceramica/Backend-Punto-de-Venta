package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolEntity, Long> {

    @EntityGraph(attributePaths = "permisos")
    Optional<RolEntity> findWithPermisosById(Long id);

    Optional<RolEntity> findByNombre(String nombre);
}
