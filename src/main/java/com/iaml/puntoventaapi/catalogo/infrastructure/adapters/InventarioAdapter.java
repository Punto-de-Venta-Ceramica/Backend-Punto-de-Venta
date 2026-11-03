package com.iaml.puntoventaapi.catalogo.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.domain.port.InventarioPort;
import com.iaml.puntoventaapi.inventario.InventarioContrato;
import org.springframework.stereotype.Component;
import java.util.Map;

// Nombre explícito: ventas tiene su propio InventarioAdapter y evita colisiones de bean.
@Component("catalogoInventarioAdapter")
public class InventarioAdapter implements InventarioPort {

    private final InventarioContrato inventario;

    public InventarioAdapter(InventarioContrato inventario) {
        this.inventario = inventario;
    }

    @Override
    public void registrarExistenciasIniciales(Long productoId, Map<Long, Integer> existenciaPorSucursal) {
        inventario.registrarExistenciasIniciales(productoId, existenciaPorSucursal);
    }
}
