package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.ventas.domain.port.NegocioPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("ventasNegocioAdapter")
public class NegocioAdapter implements NegocioPort {

    private final NegociosContrato negocios;

    public NegocioAdapter(NegociosContrato negocios) {
        this.negocios = negocios;
    }

    @Override
    public Optional<String> eslogan(Long negocioId) {
        return negocios.eslogan(negocioId);
    }
}
