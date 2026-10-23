package com.iaml.puntoventaapi.sucursal.domain.port;

public interface NegocioPort {

    boolean perteneceAUsuario(Long negocioId, Long usuarioId);
}

