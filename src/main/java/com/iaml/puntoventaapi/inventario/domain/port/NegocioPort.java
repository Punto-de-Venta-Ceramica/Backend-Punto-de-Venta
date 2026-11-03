package com.iaml.puntoventaapi.inventario.domain.port;

public interface NegocioPort {

    boolean perteneceAUsuario(Long negocioId, Long usuarioId);
}
