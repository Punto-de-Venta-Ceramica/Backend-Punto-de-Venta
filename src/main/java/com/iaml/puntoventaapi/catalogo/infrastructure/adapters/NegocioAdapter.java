package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Nombre explícito: sucursal ya define un bean "negocioAdapter".
@Component("catalogoNegocioAdapter")
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
