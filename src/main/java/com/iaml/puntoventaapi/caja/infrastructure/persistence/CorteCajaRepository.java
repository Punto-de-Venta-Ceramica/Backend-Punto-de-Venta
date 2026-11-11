package com.iaml.puntoventaapi.caja.infrastructure.persistence;

import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorteCajaRepository extends JpaRepository<CorteCaja, Long> {

    Optional<CorteCaja> findByCaja_Sucursal_IdAndFechaHoraCierreIsNull(Long sucursalId);

    Optional<CorteCaja> findByCaja_IdAndFechaHoraCierreIsNull(Long cajaId);

    List<CorteCaja> findByCaja_Sucursal_IdOrderByFechaHoraAperturaDesc(Long sucursalId);

    boolean existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(Long sucursalId);
}
