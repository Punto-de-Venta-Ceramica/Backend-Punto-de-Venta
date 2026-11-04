package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.domain.port.SucursalPort;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Component("catalogoSucursalAdapter")
public class SucursalAdapter implements SucursalPort {

    private final SucursalContrato sucursales;

    public SucursalAdapter(SucursalContrato sucursales) {
        this.sucursales = sucursales;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsParaRegistrarProductos(Long negocioId, Long usuarioId) {
        return sucursales.idsParaRegistrarProductos(negocioId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> negocioAutorizado(Long sucursalId, Long usuarioId) {
        return sucursales.negocioAutorizado(sucursalId, usuarioId);
    }
}
