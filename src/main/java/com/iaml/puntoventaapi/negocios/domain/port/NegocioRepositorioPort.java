package com.iaml.puntoventaapi.negocios.domain.port;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import java.util.List;
import java.util.Optional;

public interface NegocioRepositorioPort {
    Negocio guardar(Negocio negocio, Long usuarioId);
    Optional<Negocio> buscarPorId(Long negocioId, Long usuarioId);
    List<NegocioResumen> listarPorUsuario(Long usuarioId);
    boolean perteneceAUsuario(Long negocioId, Long usuarioId);
}
