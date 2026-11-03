package com.iaml.puntoventaapi.sucursal.application;

import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import com.iaml.puntoventaapi.sucursal.domain.port.SucursalRepositorioPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SucursalContratoService implements SucursalContrato {

    private final SucursalRepositorioPort sucursales;

    public SucursalContratoService(SucursalRepositorioPort sucursales) {
        this.sucursales = sucursales;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsPorNegocio(Long negocioId, Long usuarioId) {
        return sucursales.buscarPorNegocioId(negocioId, usuarioId).stream().map(Sucursal::getId).toList();
    }
}
