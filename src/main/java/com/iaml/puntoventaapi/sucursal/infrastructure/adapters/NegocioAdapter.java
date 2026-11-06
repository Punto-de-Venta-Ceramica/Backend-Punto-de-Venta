package com.iaml.puntoventaapi.sucursal.infrastructure.adapters;

import com.iaml.puntoventaapi.sucursal.domain.port.NegocioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NegocioAdapter implements NegocioPort {

    private final NegociosContrato negocios;

    public NegocioAdapter(NegociosContrato negocios) {
        this.negocios = negocios;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return negocios.perteneceAUsuario(negocioId, usuarioId);
    }
}

