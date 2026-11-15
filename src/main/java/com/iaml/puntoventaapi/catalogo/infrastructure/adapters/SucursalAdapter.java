package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.domain.port.SucursalPort;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component("catalogoSucursalAdapter")
public class SucursalAdapter implements SucursalPort {

    private final SucursalContrato sucursales;

    public SucursalAdapter(SucursalContrato sucursales) {
        this.sucursales = sucursales;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsPorNegocio(Long negocioId, Long usuarioId) {
        return sucursales.idsPorNegocio(negocioId, usuarioId);
    }
}
