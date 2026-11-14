package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.shared.infrastructure.adapters.NegocioAdapterBase;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

// Nombre explícito: sucursal ya define un bean "negocioAdapter".
@Component("catalogoNegocioAdapter")
public class NegocioAdapter extends NegocioAdapterBase implements NegocioPort {

    public NegocioAdapter(NegociosContrato negocios, SucursalContrato sucursales) {
        super(negocios, sucursales);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> negocioDeSucursal(Long sucursalId, Long usuarioId) {
        return negocioAutorizado(sucursalId, usuarioId);
    }
}
