package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacionEntity, Long> {

    Optional<CodigoRecuperacionEntity> findTopByCorreoOrderByExpiraEnDesc(String correo);

    @Modifying
    @Query("delete from CodigoRecuperacionEntity c where c.correo = :correo")
    void deleteByCorreo(@Param("correo") String correo);
}

