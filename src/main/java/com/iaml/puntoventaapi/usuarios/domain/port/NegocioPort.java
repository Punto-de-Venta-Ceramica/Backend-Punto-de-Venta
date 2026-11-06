package com.iaml.puntoventaapi.usuarios.domain.port;

public interface NegocioPort {

    boolean perteneceAUsuario(Long negocioId, Long usuarioId);
}

