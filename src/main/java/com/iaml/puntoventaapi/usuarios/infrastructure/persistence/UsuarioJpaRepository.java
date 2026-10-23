package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    // Misma normalización que el índice único uq_usuario_correo.
    @Query("select u from UsuarioEntity u where lower(trim(u.correo)) = lower(trim(:correo))")
    Optional<UsuarioEntity> findByCorreoNormalizado(@Param("correo") String correo);
}
