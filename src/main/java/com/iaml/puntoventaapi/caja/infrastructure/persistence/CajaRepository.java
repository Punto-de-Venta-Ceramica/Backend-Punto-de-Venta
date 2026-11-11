package com.iaml.puntoventaapi.caja.infrastructure.persistence;

import com.iaml.puntoventaapi.caja.domain.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Long> {
    Optional<Caja> findBySucursal_Id(Long sucursalId);
}
