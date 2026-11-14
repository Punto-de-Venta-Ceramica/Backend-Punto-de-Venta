package com.iaml.puntoventaapi.inventario.infrastructure.adapters;

import com.iaml.puntoventaapi.inventario.domain.port.NegocioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.shared.infrastructure.adapters.NegocioAdapterBase;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Nombre explícito: sucursal ya define un bean "negocioAdapter".
@Component("inventarioNegocioAdapter")
public class NegocioAdapter extends NegocioAdapterBase implements NegocioPort {

    public NegocioAdapter(NegociosContrato negocios, SucursalContrato sucursales) {
        super(negocios, sucursales);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeOperarSucursal(Long negocioId, Long sucursalId, Long usuarioId) {
        return negocioAutorizado(sucursalId, usuarioId).filter(negocioId::equals).isPresent();
    }
}
