package com.iaml.puntoventaapi.caja.infrastructure.persistence;

import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorteCajaRepository extends JpaRepository<CorteCaja, Long> {

    Optional<CorteCaja> findByCaja_Sucursal_IdAndFechaHoraCierreIsNull(Long sucursalId);

    @Query("select c.id from CorteCaja c where c.caja.sucursal.id = :sucursalId and c.fechaHoraCierre is null")
    Optional<Long> buscarIdAbierto(@Param("sucursalId") Long sucursalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CorteCaja c where c.id = :id")
    Optional<CorteCaja> buscarConBloqueo(@Param("id") Long id);

    Optional<CorteCaja> findByCaja_IdAndFechaHoraCierreIsNull(Long cajaId);

    List<CorteCaja> findByCaja_Sucursal_IdOrderByFechaHoraAperturaDesc(Long sucursalId);

    Page<CorteCaja> findByCaja_Sucursal_IdOrderByFechaHoraAperturaDesc(Long sucursalId, Pageable pageable);

    Page<CorteCaja> findByCaja_Sucursal_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(Long sucursalId, Pageable pageable);

    Page<CorteCaja> findByCaja_Sucursal_IdAndUsuario_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(Long sucursalId, Long usuarioId, Pageable pageable);

    Page<CorteCaja> findByUsuario_IdAndCaja_Sucursal_IdInAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(Long usuarioId, java.util.Collection<Long> sucursalIds, Pageable pageable);

    Page<CorteCaja> findByCaja_Sucursal_Negocio_IdOrderByFechaHoraAperturaDesc(Long negocioId, Pageable pageable);

    Page<CorteCaja> findByCaja_Sucursal_Negocio_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(Long negocioId, Pageable pageable);

    boolean existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(Long sucursalId);
}
