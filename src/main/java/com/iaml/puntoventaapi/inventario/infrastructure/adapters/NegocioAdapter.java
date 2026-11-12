package com.iaml.puntoventaapi.inventario.infrastructure.adapters;

import com.iaml.puntoventaapi.inventario.domain.port.NegocioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Nombre explícito: sucursal ya define un bean "negocioAdapter".
@Component("inventarioNegocioAdapter")
public class NegocioAdapter implements NegocioPort {

    private final NegociosContrato negocios;
    private final SucursalContrato sucursales;

    public NegocioAdapter(NegociosContrato negocios, SucursalContrato sucursales) {
        this.negocios = negocios;
        this.sucursales = sucursales;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return negocios.perteneceAUsuario(negocioId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeOperarSucursal(Long negocioId, Long sucursalId, Long usuarioId) {
        return sucursales.negocioAutorizado(sucursalId, usuarioId)
            .filter(negocioId::equals)
            .isPresent();
    }
}
