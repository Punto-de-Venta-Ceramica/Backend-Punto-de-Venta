package com.iaml.puntoventaapi.negocios;

public interface NegociosContrato {
    boolean perteneceAUsuario(Long negocioId, Long usuarioId);
}
