package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SucursalJpaRepository extends JpaRepository<Sucursal, Long> {

    Optional<Sucursal> findByIdAndNegocio_Usuarios_Id(Long id, Long usuarioId);

    List<Sucursal> findByNegocio_IdAndNegocio_Usuarios_IdOrderByIdAsc(Long negocioId, Long usuarioId);
}

