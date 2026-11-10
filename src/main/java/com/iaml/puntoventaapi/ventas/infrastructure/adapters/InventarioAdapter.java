package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.inventario.InventarioContrato;
import com.iaml.puntoventaapi.ventas.domain.port.InventarioPort;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("ventasInventarioAdapter")
public class InventarioAdapter implements InventarioPort {

    private final InventarioContrato inventario;

    public InventarioAdapter(InventarioContrato inventario) {
        this.inventario = inventario;
    }

    @Override
    public Map<Long, Integer> consultarExistencias(Long negocioId, Long sucursalId, Long usuarioId) {
        return inventario.consultarExistencias(negocioId, sucursalId, usuarioId);
    }
}
