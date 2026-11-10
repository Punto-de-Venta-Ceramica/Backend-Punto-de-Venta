package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.ventas.domain.port.SucursalPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("ventasSucursalAdapter")
public class SucursalAdapter implements SucursalPort {

    private final SucursalContrato sucursales;

    public SucursalAdapter(SucursalContrato sucursales) {
        this.sucursales = sucursales;
    }

    @Override
    public Optional<Long> obtenerNegocioIdAccesible(Long sucursalId, Long usuarioId) {
        return sucursales.obtenerNegocioIdAccesible(sucursalId, usuarioId);
    }
}
