package com.iaml.puntoventaapi.negocios.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    List<NegocioResumen> findAllByUsuarios_IdOrderByNombreAscIdAsc(Long usuarioId);

    Optional<Negocio> findByIdAndUsuarios_Id(Long negocioId, Long usuarioId);
}
