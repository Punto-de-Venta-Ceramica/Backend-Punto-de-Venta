package com.iaml.puntoventaapi.usuarios.infrastructure.adapters;

import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.usuarios.domain.port.NegocioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("usuarioNegocioAdapter")
@RequiredArgsConstructor
public class NegocioAdapter implements NegocioPort {

    private final NegociosContrato negociosContrato;

    @Override
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return negociosContrato.perteneceAUsuario(negocioId, usuarioId);
    }
}

