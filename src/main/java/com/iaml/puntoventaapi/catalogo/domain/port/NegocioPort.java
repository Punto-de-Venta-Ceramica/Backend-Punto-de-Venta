package com.iaml.puntoventaapi.catalogo.domain.port;

public interface NegocioPort {

    boolean perteneceAUsuario(Long negocioId, Long usuarioId);
}
