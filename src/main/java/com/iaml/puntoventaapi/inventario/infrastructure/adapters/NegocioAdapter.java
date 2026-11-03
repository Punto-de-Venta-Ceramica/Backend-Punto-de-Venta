package com.iaml.puntoventaapi.inventario.infrastructure.adapters;

import com.iaml.puntoventaapi.inventario.domain.port.NegocioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Nombre explícito: sucursal ya define un bean "negocioAdapter".
@Component("inventarioNegocioAdapter")
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
