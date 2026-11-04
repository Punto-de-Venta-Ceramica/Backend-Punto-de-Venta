package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AreaJpaRepository extends JpaRepository<AreaEntity, Long> {

    @Query("""
        select a from AreaEntity a
        where a.id = :areaId and a.negocio.id = :negocioId
          and a.activo = true and a.negocio.activo = true
        """)
    Optional<AreaEntity> buscarActivaPorId(@Param("areaId") Long areaId, @Param("negocioId") Long negocioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AreaEntity a where a.id = :areaId and a.activo = true and a.negocio.activo = true")
    Optional<AreaEntity> bloquearActivaPorId(@Param("areaId") Long areaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from AreaEntity a
        where a.id = :areaId and a.negocio.id = :negocioId
          and a.activo = true and a.negocio.activo = true
        """)
    Optional<AreaEntity> bloquearActivaPorId(@Param("areaId") Long areaId, @Param("negocioId") Long negocioId);

    @Query("""
        select a from AreaEntity a
        where a.negocio.id = :negocioId and a.activo = true and a.negocio.activo = true
        order by a.nombre, a.id
        """)
    List<AreaEntity> listarActivasPorNegocio(@Param("negocioId") Long negocioId);
}
